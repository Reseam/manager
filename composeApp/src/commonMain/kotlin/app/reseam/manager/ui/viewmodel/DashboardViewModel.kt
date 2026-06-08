package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.InstalledAppSource
import app.reseam.manager.ui.model.AppView
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.InstalledAppSummary
import app.reseam.manager.ui.model.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class DashboardViewModel internal constructor(
    private val store: ManagerStateStore,
    private val installedApps: InstalledAppSource,
    private val patchedApps: PatchedAppStore,
    private val bundles: BundleStore,
    private val patchStore: PatchStore,
    private val bundleImporter: BundleImporter?,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    fun load() {
        scope.launch {
            store.update { it.copy(busy = true, error = null) }
            val local = try {
                applyLocalPhase()
            } catch (error: Throwable) {
                store.update {
                    it.copy(
                        busy = false,
                        error = "Could not load manager state: ${error.message ?: error::class.simpleName}",
                    )
                }
                return@launch
            }
            try {
                syncOfficialPhase(local)
                store.update { it.copy(busy = false) }
            } catch (error: Throwable) {
                store.update {
                    it.copy(busy = false, error = backgroundSyncMessage(local.bundles, error))
                }
            }
        }
    }

    private suspend fun applyLocalPhase(): LocalSnapshot {
        val patched = patchedApps.list()
        val savedSettings = settings.load()
        val installedBundles = bundles.list().filter { it.path != null }
        val countByPackage = patchStore.compatibleCountByPackage()
        val apps = installedAppsWithCounts(countByPackage)
        store.update {
            it.copy(
                backStack = listOf(AppView.Home),
                home = it.home.copy(patchedApps = patched, installedApps = apps),
                bundles = it.bundles.copy(installed = installedBundles),
                settings = savedSettings,
            )
        }
        return LocalSnapshot(savedSettings, installedBundles)
    }

    private suspend fun syncOfficialPhase(local: LocalSnapshot) {
        val importer = bundleImporter ?: return
        val current = local.bundles.firstOrNull { it.official }
        if (current != null && !local.settings.checkUpdatesDaily) return
        val result = importer.syncOfficial(local.settings.apiBaseUrl, current?.version)
        if (result == null) {
            check(current != null) { "Official bundle index returned no release" }
            return
        }
        bundles.save(result.summary)
        patchStore.replaceForBundle(result.summary.id, result.patches)
        val refreshedBundles = bundles.list().filter { it.path != null }
        val refreshedCounts = patchStore.compatibleCountByPackage()
        val refreshedApps = installedAppsWithCounts(refreshedCounts)
        store.update {
            it.copy(
                home = it.home.copy(installedApps = refreshedApps),
                bundles = it.bundles.copy(installed = refreshedBundles),
            )
        }
    }

    private suspend fun installedAppsWithCounts(
        countByPackage: Map<String, Int>,
    ): List<InstalledAppSummary> = installedApps.apps(countByPackage.keys)
        .map { app -> app.copy(compatiblePatchCount = countByPackage[app.packageName] ?: 0) }
        .sortedWith(
            compareByDescending<InstalledAppSummary> { it.compatiblePatchCount ?: 0 }
                .thenBy { it.name.lowercase() },
        )

    private fun backgroundSyncMessage(installed: List<BundleSummary>, error: Throwable): String? = when {
        installed.isNotEmpty() -> null
        else -> "Could not download the official patch bundle: ${error.message ?: error::class.simpleName}"
    }
}

private data class LocalSnapshot(
    val settings: SettingsState,
    val bundles: List<BundleSummary>,
)
