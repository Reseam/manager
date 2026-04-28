package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.InstalledAppSource
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.InputMode
import app.reseam.manager.ui.model.InstalledAppSummary
import app.reseam.manager.ui.model.LoadState
import app.reseam.manager.ui.model.PatchEditorState
import app.reseam.manager.ui.model.PatchRunState
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.model.SettingsState
import app.reseam.manager.ui.model.navigation.ManagerRoute
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
            store.setBusy(true)
            store.clearError()
            runCatching { loadSnapshot() }
                .onSuccess { snapshot -> applySnapshot(snapshot) }
                .onFailure { error ->
                    store.update {
                        it.copy(
                            busy = false,
                            error = "Could not load manager state: ${error.message ?: error::class.simpleName}",
                        )
                    }
                }
        }
    }

    private suspend fun loadSnapshot(): DashboardSnapshot {
        val patched = patchedApps.list()
        val savedSettings = settings.load()
        val (installedBundles, bootstrapError) = loadBundles(savedSettings)
        val countByPackage = patchStore.compatibleCountByPackage()
        val apps = installedApps.installedApps()
            .map { app ->
                app.copy(compatiblePatchCount = app.compatiblePatchCount ?: countByPackage[app.packageName] ?: 0)
            }
            .sortedWith(
                compareByDescending<InstalledAppSummary> { it.compatiblePatchCount ?: 0 }
                    .thenBy { it.name.lowercase() },
            )

        return DashboardSnapshot(
            patchedApps = patched,
            bundles = installedBundles,
            settings = savedSettings,
            installedApps = apps,
            bootstrapError = bootstrapError,
        )
    }

    private fun applySnapshot(snapshot: DashboardSnapshot) {
        store.update {
            it.copy(
                navigation = it.navigation.reset(ManagerRoute.Home),
                home = it.home.copy(patchedApps = snapshot.patchedApps),
                bundles = it.bundles.copy(installed = snapshot.bundles),
                settings = snapshot.settings,
                flow = it.flow.copy(installedApps = snapshot.installedApps),
                busy = false,
                error = bootstrapErrorMessage(snapshot.bundles, snapshot.bootstrapError),
            )
        }
    }

    private suspend fun loadBundles(savedSettings: SettingsState): Pair<List<BundleSummary>, Throwable?> {
        val installed = bundles.list().filter { it.path != null }
        val importer = bundleImporter ?: return installed to null
        val current = installed.firstOrNull { it.official }

        if (current == null) {
            return try {
                val result = importer.syncOfficial(savedSettings.apiBaseUrl, currentVersion = null)
                    ?: throw IllegalStateException("Official bundle index returned no release")
                bundles.save(result.summary)
                patchStore.replaceForBundle(result.summary.id, result.patches)
                (installed + result.summary) to null
            } catch (error: Throwable) {
                installed to error
            }
        }

        if (!savedSettings.checkUpdatesDaily) return installed to null

        return try {
            val updated = importer.syncOfficial(savedSettings.apiBaseUrl, current.version)
            if (updated == null) {
                installed to null
            } else {
                bundles.save(updated.summary)
                patchStore.replaceForBundle(updated.summary.id, updated.patches)
                installed.map { if (it.id == updated.summary.id) updated.summary else it } to null
            }
        } catch (error: Throwable) {
            installed to null
        }
    }

    private fun bootstrapErrorMessage(installed: List<BundleSummary>, error: Throwable?): String? = when {
        installed.isNotEmpty() -> null
        error != null -> "Could not download the official patch bundle: ${error.message ?: error::class.simpleName}"
        else -> "Could not download the official patch bundle. Check your connection and open Bundles to retry."
    }

    fun startNewPatch() {
        store.update {
            it.copy(
                navigation = it.navigation.reset().push(ManagerRoute.Inputs),
                error = null,
                flow = it.flow.copy(
                    inputMode = InputMode.Installed,
                    searchQuery = "",
                    selectedInput = null,
                    inspect = LoadState.Idle,
                    editor = PatchEditorState(),
                    run = PatchRunState(),
                ),
            )
        }
    }

    fun openSettings() {
        store.navigate { it.push(ManagerRoute.Settings) }
    }

    fun openBundles() {
        store.navigate { it.push(ManagerRoute.Bundles) }
    }

    fun openPatchedApp(appId: String) {
        store.navigate { it.push(ManagerRoute.AppDetail(appId)) }
    }

}

private data class DashboardSnapshot(
    val patchedApps: List<PatchedAppSummary>,
    val bundles: List<BundleSummary>,
    val settings: SettingsState,
    val installedApps: List<InstalledAppSummary>,
    val bootstrapError: Throwable?,
)
