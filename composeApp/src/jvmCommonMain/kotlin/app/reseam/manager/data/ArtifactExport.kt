package app.reseam.manager.data

import app.reseam.manager.platform.enginePath
import app.reseam.sdk.writeSplitArchive
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies [artifact] to [destination]: a single APK as it is, a split build as one APKS file. The archive is written
 * beside the app first, since [destination] may be a document only the platform can open.
 */
suspend fun exportArtifact(artifact: PlatformFile, destination: PlatformFile) = withContext(Dispatchers.IO) {
    if (!artifact.exists()) throw Failure.OutputMissing()
    val sourcePath = artifact.enginePath
    val destinationPath = destination.enginePath
    if (sourcePath != null && destinationPath != null && File(sourcePath).canonicalFile == File(destinationPath).canonicalFile) return@withContext
    if (!artifact.isDirectory()) {
        artifact.copyTo(destination)
        return@withContext
    }
    val apks = artifact.list().filter { it.name.endsWith(".apk", ignoreCase = true) }.sortedBy { it.name }
    if (apks.isEmpty()) throw Failure.OutputMissing()
    val staging = Files.createTempDirectory("reseam-export").toFile()
    try {
        val archive = staging.resolve("${artifact.name}.apks")
        writeSplitArchive(apks.map { it.absolutePath() }, archive.path)
        PlatformFile(archive).copyTo(destination)
    } finally {
        staging.deleteRecursively()
    }
}
