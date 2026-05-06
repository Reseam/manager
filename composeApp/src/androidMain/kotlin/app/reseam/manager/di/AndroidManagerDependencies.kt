package app.reseam.manager.di

import android.content.Context
import android.system.Os
import app.reseam.manager.data.db.createReseamDatabase
import app.reseam.manager.data.platform.AndroidBundleImporter
import app.reseam.manager.data.platform.AndroidInstalledAppSource
import app.reseam.manager.data.platform.AndroidPatchedAppInstaller
import app.reseam.manager.data.platform.AndroidReseamBackend
import app.reseam.manager.data.repository.SqlBundleStore
import app.reseam.manager.data.repository.SqlPatchStore
import app.reseam.manager.data.repository.SqlPatchedAppStore
import app.reseam.manager.data.repository.SqlSettingsStore
import app.reseam.manager.domain.manager.DefaultOutputPathProvider
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.ui.viewmodel.ManagerViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope

fun createAndroidManagerViewModel(
    context: Context,
    scope: CoroutineScope,
    backend: AndroidReseamBackend = AndroidReseamBackend(),
): ManagerViewModel {
    installNativeTempDirectory(context)
    when (val result = backend.installPatchClassLoader(context.classLoader)) {
        is ReseamCallResult.Failure -> error("Could not initialize patcher class loader: ${result.message}")
        is ReseamCallResult.Success -> Unit
    }

    val database = createReseamDatabase(context)

    return ManagerViewModel(
        backend = backend,
        installedApps = AndroidInstalledAppSource(context),
        patchedApps = SqlPatchedAppStore(database),
        bundleStore = SqlBundleStore(database),
        patchStore = SqlPatchStore(database),
        settingsStore = SqlSettingsStore(database),
        bundleImporter = AndroidBundleImporter(context, backend),
        installer = AndroidPatchedAppInstaller(context),
        outputPaths = DefaultOutputPathProvider(File(context.cacheDir, "reseam/output").also { it.mkdirs() }.absolutePath),
        scope = scope,
    )
}

private fun installNativeTempDirectory(context: Context) {
    val tempDir = context.cacheDir.also { it.mkdirs() }.absolutePath
    runCatching {
        Os.setenv("TMPDIR", tempDir, true)
        Os.setenv("TMP", tempDir, true)
        Os.setenv("TEMP", tempDir, true)
    }
}
