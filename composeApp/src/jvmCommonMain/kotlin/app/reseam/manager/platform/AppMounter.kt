package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

data class ApkSet(val versionCode: Long, val base: String, val splits: Map<String, String>)

interface AppMounter {
    suspend fun requestAccess(): Boolean

    suspend fun matches(packageName: String, apks: ApkSet): Boolean

    suspend fun install(packageName: String, apks: ApkSet)

    suspend fun mount(packageName: String, apks: ApkSet)

    suspend fun unmount(packageName: String)

    suspend fun isMounted(packageName: String): Boolean

    suspend fun copyOriginals(packageName: String, directory: PlatformFile): List<PlatformFile>
}
