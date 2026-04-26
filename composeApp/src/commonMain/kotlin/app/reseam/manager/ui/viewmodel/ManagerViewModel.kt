package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.data.repository.InMemoryBundleStore
import app.reseam.manager.data.repository.InMemoryPatchStore
import app.reseam.manager.data.repository.InMemoryPatchedAppStore
import app.reseam.manager.data.repository.InMemorySettingsStore
import app.reseam.manager.domain.installer.PatchedAppInstaller
import app.reseam.manager.domain.manager.DefaultOutputPathProvider
import app.reseam.manager.domain.manager.OutputPathProvider
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.InstalledAppSource
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.patcher.ReseamManagerCore
import app.reseam.manager.ui.model.ManagerUiState
import kotlinx.coroutines.CoroutineScope

@Stable
class ManagerViewModel(
    backend: ReseamBackend,
    installedApps: InstalledAppSource,
    patchedApps: PatchedAppStore = InMemoryPatchedAppStore(),
    bundleStore: BundleStore = InMemoryBundleStore(),
    patchStore: PatchStore = InMemoryPatchStore(),
    settingsStore: SettingsStore = InMemorySettingsStore(),
    bundleImporter: BundleImporter? = null,
    installer: PatchedAppInstaller? = null,
    outputPaths: OutputPathProvider = DefaultOutputPathProvider("/tmp"),
    scope: CoroutineScope,
) {
    private val store = ManagerStateStore()
    private val core = ReseamManagerCore(backend)

    val navigation = NavigationViewModel(store)
    val home = DashboardViewModel(store, installedApps, patchedApps, bundleStore, patchStore, bundleImporter, settingsStore, scope)
    val appDetail = AppDetailViewModel(store)
    val inputs = InputsViewModel(store, core, patchStore, scope)
    val patches = PatchesViewModel(store)
    val run = PatchRunViewModel(store, core, patchedApps, installer, outputPaths, scope)
    val settings = SettingsViewModel(store, settingsStore, scope)
    val bundles = BundlesViewModel(store, bundleStore, patchStore, bundleImporter, scope)
    val bundleDetail = BundleDetailViewModel(store, bundleStore, patchStore, scope)

    val state: ManagerUiState
        get() = store.state
}
