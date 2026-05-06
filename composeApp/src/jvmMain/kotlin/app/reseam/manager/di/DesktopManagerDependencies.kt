package app.reseam.manager.di

import app.reseam.manager.data.db.createReseamDatabase
import app.reseam.manager.data.platform.DesktopBundleImporter
import app.reseam.manager.data.platform.DesktopInstalledAppSource
import app.reseam.manager.data.platform.DesktopPatchedAppInstaller
import app.reseam.manager.data.platform.DesktopReseamBackend
import app.reseam.manager.data.platform.desktopDataFile
import app.reseam.manager.data.repository.SqlBundleStore
import app.reseam.manager.data.repository.SqlPatchStore
import app.reseam.manager.data.repository.SqlPatchedAppStore
import app.reseam.manager.data.repository.SqlSettingsStore
import app.reseam.manager.domain.manager.DefaultOutputPathProvider
import app.reseam.manager.ui.viewmodel.ManagerViewModel
import kotlinx.coroutines.CoroutineScope

fun createDesktopManagerViewModel(
    scope: CoroutineScope,
    backend: DesktopReseamBackend = DesktopReseamBackend(),
): ManagerViewModel {
    val database = createReseamDatabase()

    return ManagerViewModel(
        backend = backend,
        installedApps = DesktopInstalledAppSource(),
        patchedApps = SqlPatchedAppStore(database),
        bundleStore = SqlBundleStore(database),
        patchStore = SqlPatchStore(database),
        settingsStore = SqlSettingsStore(database),
        bundleImporter = DesktopBundleImporter(backend),
        installer = DesktopPatchedAppInstaller(),
        outputPaths = DefaultOutputPathProvider(desktopDataFile("output").also { it.mkdirs() }.absolutePath),
        scope = scope,
    )
}
