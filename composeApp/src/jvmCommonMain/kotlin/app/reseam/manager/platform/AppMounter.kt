package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

data class ApkSet(val versionCode: Int, val base: String, val splits: Map<String, String>)

interface AppMounter {
    suspend fun requestAccess(): Boolean

    /** Installs [apks], the app's original, as an update of the app on the device. */
    suspend fun install(packageName: String, apks: ApkSet)

    /**
     * Mounts the patched [apks] over the installed app. Fails with the SDK's `InstalledAppDiffers` or `AppNotInstalled`
     * when the installed app is not the original the build was made from.
     */
    suspend fun mount(packageName: String, apks: ApkSet)

    suspend fun unmount(packageName: String)

    suspend fun isMounted(packageName: String): Boolean

    suspend fun copyOriginals(packageName: String, directory: PlatformFile): List<PlatformFile>
}
