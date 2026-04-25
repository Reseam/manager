package app.reseam.manager.di

import app.reseam.manager.data.platform.DesktopBundleImporter
import app.reseam.manager.data.platform.DesktopInstalledAppSource
import app.reseam.manager.data.platform.DesktopOutputPathProvider
import app.reseam.manager.data.platform.DesktopPatchedAppInstaller
import app.reseam.manager.data.platform.DesktopReseamBackend
import app.reseam.manager.data.repository.DesktopBundleStore
import app.reseam.manager.data.repository.DesktopPatchedAppStore
import app.reseam.manager.data.repository.DesktopSettingsStore
import app.reseam.manager.ui.viewmodel.ManagerViewModel
import kotlinx.coroutines.CoroutineScope

fun createDesktopManagerViewModel(
    scope: CoroutineScope,
    backend: DesktopReseamBackend = DesktopReseamBackend(),
): ManagerViewModel =
    ManagerViewModel(
        backend = backend,
        installedApps = DesktopInstalledAppSource(),
        patchedApps = DesktopPatchedAppStore(),
        bundles = DesktopBundleStore(),
        settings = DesktopSettingsStore(),
        bundleImporter = DesktopBundleImporter(backend),
        installer = DesktopPatchedAppInstaller(),
        outputPaths = DesktopOutputPathProvider(),
        scope = scope,
    )
