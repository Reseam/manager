package app.reseam.manager.data

import app.reseam.manager.platform.ApkPresentationReader
import app.reseam.manager.sdk.openApkArchive
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlin.uuid.Uuid

/** Who a picked app is, independent of its file name. The icon is kept as a file so route keys and the library can carry it. */
data class AppIdentity(val name: String, val packageName: String?, val versionName: String?, val iconPath: String?)

class AppIdentityReader(private val iconDirectory: PlatformFile, private val presentation: ApkPresentationReader) {
    suspend fun read(apkPath: String): AppIdentity = withContext(Dispatchers.IO) {
        openApkArchive(apkPath).use { archive ->
            val shown = presentation.read(archive)
            AppIdentity(
                name = shown.label?.takeIf { it.isNotBlank() } ?: archive.metadata.packageName ?: "Unnamed app",
                packageName = archive.metadata.packageName,
                versionName = archive.metadata.versionName,
                iconPath = shown.icon?.let(::store),
            )
        }
    }

    private fun store(icon: ByteArray): String {
        iconDirectory.createDirectories()
        val file = iconDirectory / Uuid.random().toString()
        file.sink().buffered().use { it.write(icon) }
        return file.absolutePath()
    }
}
