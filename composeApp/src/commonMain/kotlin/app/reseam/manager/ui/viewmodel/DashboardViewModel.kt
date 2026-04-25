package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.domain.sources.InstalledAppSource
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.InputMode
import app.reseam.manager.ui.model.LoadState
import app.reseam.manager.ui.model.PatchEditorState
import app.reseam.manager.ui.model.PatchRunState
import app.reseam.manager.ui.model.navigation.ManagerRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class DashboardViewModel internal constructor(
    private val store: ManagerStateStore,
    private val installedApps: InstalledAppSource,
    private val patchedApps: PatchedAppStore,
    private val bundles: BundleStore,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    fun load() {
        scope.launch {
            store.update { it.copy(busy = true, error = null) }
            val patched = patchedApps.list()
            val installedBundles = bundles.list().ifEmpty { listOf(BundleSummary.official()) }
            val savedSettings = settings.load()
            val apps = installedApps.installedApps()

            store.update {
                it.copy(
                    navigation = it.navigation.reset(ManagerRoute.Home),
                    home = it.home.copy(patchedApps = patched),
                    bundles = it.bundles.copy(installed = installedBundles),
                    settings = savedSettings,
                    flow = it.flow.copy(installedApps = apps),
                    busy = false,
                )
            }
        }
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
