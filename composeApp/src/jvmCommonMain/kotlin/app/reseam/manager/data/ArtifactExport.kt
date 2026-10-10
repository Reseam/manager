package app.reseam.manager.data

import app.reseam.manager.platform.enginePath
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asInputStream
import kotlinx.io.asOutputStream
import kotlinx.io.buffered

suspend fun exportArtifact(artifact: PlatformFile, destination: PlatformFile) = withContext(Dispatchers.IO) {
    if (!artifact.exists()) throw Failure.OutputMissing()
    val sourcePath = artifact.enginePath
    val destinationPath = destination.enginePath
    if (sourcePath != null && destinationPath != null && File(sourcePath).canonicalFile == File(destinationPath).canonicalFile) return@withContext
    if (!artifact.isDirectory()) {
        artifact.copyTo(destination)
    } else {
        val apks = artifact.list().filter { it.name.endsWith(".apk", ignoreCase = true) }.sortedBy { it.name }
        if (apks.isEmpty()) throw Failure.OutputMissing()
        ZipOutputStream(destination.sink().buffered().asOutputStream()).use { zip ->
            apks.forEach { apk ->
                zip.putNextEntry(ZipEntry(apk.name))
                apk.source().buffered().asInputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}
