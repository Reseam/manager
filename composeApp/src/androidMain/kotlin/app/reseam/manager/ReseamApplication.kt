package app.reseam.manager

import android.app.Application
import android.os.Build
import android.system.Os
import app.reseam.manager.platform.AndroidApkPresentationReader
import app.reseam.manager.platform.AndroidBackgroundRun
import app.reseam.manager.platform.AndroidInstalledApps
import app.reseam.manager.platform.AndroidInstaller
import app.reseam.manager.platform.AndroidSourceSession
import app.reseam.manager.platform.ApkManagerInstaller
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.RootAppMounter
import app.reseam.sdk.ReseamAndroidHost
import io.github.vinceglb.filekit.PlatformFile

class ReseamApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        // The engine writes scratch files through the C temp dir, which Android leaves unset.
        Os.setenv("TMPDIR", cacheDir.absolutePath, true)
        ReseamAndroidHost.setClassLoader(classLoader)
        val installer = AndroidInstaller(this)
        graph = AppGraph(
            dataDirectory = PlatformFile(filesDir),
            cacheDirectory = PlatformFile(cacheDir),
            installedApps = AndroidInstalledApps(this),
            artifactAction = installer,
            mounter = RootAppMounter(this),
            presentation = AndroidApkPresentationReader(this),
            sourceSession = AndroidSourceSession(this),
            device = DeviceProfile("${Build.MANUFACTURER} ${Build.MODEL}", Build.SUPPORTED_ABIS.toList(), Build.VERSION.SDK_INT, resources.displayMetrics.densityDpi),
            backgroundRun = AndroidBackgroundRun(this),
            managerInstaller = ApkManagerInstaller.forDevice(installer),
        )
    }
}
