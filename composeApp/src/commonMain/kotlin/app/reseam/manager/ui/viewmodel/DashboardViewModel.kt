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
            store.update { it.copy(busy = true, error = null) }
            val patched = patchedApps.list()
            val savedSettings = settings.load()
            val (installedBundles, bootstrapError) = loadBundles(savedSettings)
            val countByPkg = patchStore.compatibleCountByPackage()
            val apps = installedApps.installedApps()
                .map { it.copy(compatiblePatchCount = it.compatiblePatchCount ?: countByPkg[it.packageName] ?: 0) }
                .sortedWith(
                    compareByDescending<InstalledAppSummary> { it.compatiblePatchCount ?: 0 }
                        .thenBy { it.name.lowercase() },
                )

            store.update {
                it.copy(
                    navigation = it.navigation.reset(ManagerRoute.Home),
                    home = it.home.copy(patchedApps = patched),
                    bundles = it.bundles.copy(installed = installedBundles),
                    settings = savedSettings,
                    flow = it.flow.copy(installedApps = apps),
                    busy = false,
                    error = bootstrapErrorMessage(installedBundles, bootstrapError),
                )
            }
        }
    }

    private suspend fun loadBundles(savedSettings: SettingsState): Pair<List<BundleSummary>, Throwable?> {
        val installed = bundles.list().filter { it.path != null }
        if (installed.isNotEmpty()) return installed to null

        val importer = bundleImporter ?: return emptyList<BundleSummary>() to null
        return try {
            val result = importer.importOfficial(savedSettings.apiBaseUrl)
            bundles.save(result.summary)
            patchStore.replaceForBundle(result.summary.id, result.patches)
            listOf(result.summary) to null
        } catch (error: Throwable) {
            error.printStackTrace()
            emptyList<BundleSummary>() to error
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
        store.update { it.copy(navigation = it.navigation.push(ManagerRoute.Settings), error = null) }
    }

    fun openBundles() {
        store.update { it.copy(navigation = it.navigation.push(ManagerRoute.Bundles), error = null) }
    }

    fun openPatchedApp(appId: String) {
        store.update { it.copy(navigation = it.navigation.push(ManagerRoute.AppDetail(appId)), error = null) }
    }

}
