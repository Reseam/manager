package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

/**
 * Mounts patched output over an installed app with root, so the app keeps its signature, data, and Google sign-in.
 * Absent on platforms without root. Failures throw with a message for the user.
 */
interface AppMounter {
    /** Whether the device offers root to this app, found without asking for it. */
    val available: Boolean

    /** Asks for root; the root manager may show a prompt. */
    suspend fun requestAccess(): Boolean

    /** Mounts [artifact] over the installed [packageName] now and after every reboot. */
    suspend fun mount(packageName: String, artifact: PlatformFile)

    /** Removes the mount; the installed app is the original again. */
    suspend fun unmount(packageName: String)

    suspend fun isMounted(packageName: String): Boolean

    /** Copies the installed app's own APKs, which its mount hides, into [directory]. The base APK comes first. */
    suspend fun copyOriginals(packageName: String, directory: PlatformFile): List<PlatformFile>
}
