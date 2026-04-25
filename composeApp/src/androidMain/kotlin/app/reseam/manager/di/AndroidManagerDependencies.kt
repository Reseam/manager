package app.reseam.manager.di

import android.content.Context
import app.reseam.manager.data.platform.AndroidBundleImporter
import app.reseam.manager.data.platform.AndroidInstalledAppSource
import app.reseam.manager.data.platform.AndroidOutputPathProvider
import app.reseam.manager.data.platform.AndroidPatchedAppInstaller
import app.reseam.manager.data.platform.AndroidReseamBackend
import app.reseam.manager.data.repository.AndroidBundleStore
import app.reseam.manager.data.repository.AndroidPatchedAppStore
import app.reseam.manager.data.repository.AndroidSettingsStore
import app.reseam.manager.ui.viewmodel.ManagerViewModel
import kotlinx.coroutines.CoroutineScope

fun createAndroidManagerViewModel(
    context: Context,
    scope: CoroutineScope,
    backend: AndroidReseamBackend = AndroidReseamBackend(),
): ManagerViewModel =
    ManagerViewModel(
        backend = backend,
        installedApps = AndroidInstalledAppSource(context),
        patchedApps = AndroidPatchedAppStore(context),
        bundles = AndroidBundleStore(context),
        settings = AndroidSettingsStore(context),
        bundleImporter = AndroidBundleImporter(context, backend),
        installer = AndroidPatchedAppInstaller(context),
        outputPaths = AndroidOutputPathProvider(context),
        scope = scope,
    )
