package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

/** An app's APK files: the base and each split by its split name, all of [versionCode]. */
data class ApkSet(val versionCode: Long, val base: String, val splits: Map<String, String>)

/**
 * Mounts patched output over an installed app with root, so the app keeps its signature, data, and Google sign-in.
 * Absent on platforms without root. Failures throw with a message for the user.
 */
interface AppMounter {
    /** Whether the device offers root to this app, found without asking for it. */
    val available: Boolean

    /** Asks for root; the root manager may show a prompt. */
    suspend fun requestAccess(): Boolean

    /** Whether the installed [packageName] has the version and splits of [apks], so they can mount over it. */
    suspend fun matches(packageName: String, apks: ApkSet): Boolean

    /** Installs [apks] as [packageName] over any installed version, older or newer, keeping the app's data. A mount comes off first. */
    suspend fun install(packageName: String, apks: ApkSet)

    /** Mounts [apks] over the installed [packageName] now and after every reboot. The installed app must [match][matches] them. */
    suspend fun mount(packageName: String, apks: ApkSet)

    /** Removes the mount; the installed app is the original again. */
    suspend fun unmount(packageName: String)

    suspend fun isMounted(packageName: String): Boolean

    /** Copies the installed app's own APKs, which its mount hides, into [directory]. The base APK comes first. */
    suspend fun copyOriginals(packageName: String, directory: PlatformFile): List<PlatformFile>
}
