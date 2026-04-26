package app.reseam.manager.data.platform

import android.content.Context
import android.net.Uri
import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.BundleImportResult
import app.reseam.manager.domain.sources.OfficialPatchesIndex
import app.reseam.manager.domain.sources.OfficialPatchesPublicKeyHex
import app.reseam.manager.domain.sources.markOfficial
import app.reseam.manager.domain.sources.officialPatchesIndexUrl
import app.reseam.manager.domain.sources.validateBundle
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.patcher.ReseamJson
import java.io.File
import java.io.InputStream
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidBundleImporter(
    private val context: Context,
    private val backend: ReseamBackend,
) : BundleImporter {
    private val bundleDirectory = File(context.filesDir, "reseam/bundles")

    override suspend fun importOfficial(apiBaseUrl: String): BundleImportResult {
        val indexJson = withContext(Dispatchers.IO) {
            URL(officialPatchesIndexUrl(apiBaseUrl)).openStream().bufferedReader().use { it.readText() }
        }
        val index = ReseamJson.codec.decodeFromString<OfficialPatchesIndex>(indexJson)
        check(index.bundle.publicKey.lowercase() == OfficialPatchesPublicKeyHex) { "Official bundle key mismatch" }
        val release = index.latestStableRelease() ?: error("Official bundle has no stable release")
        return importFromUrl(release.downloadUrl).markOfficial(release, OfficialPatchesPublicKeyHex)
    }

    override suspend fun importFromUrl(url: String): BundleImportResult =
        downloadInto(bundleFile(url.substringAfterLast('/'))) {
            URL(url).openStream()
        }.let { validate(it, source = url) }

    override suspend fun importFromFile(path: String): BundleImportResult =
        downloadInto(bundleFile(path.substringAfterLast('/'))) {
            val uri = runCatching { Uri.parse(path) }.getOrNull()
            if (uri?.scheme != null) {
                context.contentResolver.openInputStream(uri) ?: error("Could not open bundle: $path")
            } else {
                File(path).inputStream()
            }
        }.let { validate(it, source = path) }

    private fun bundleFile(name: String): File {
        bundleDirectory.mkdirs()
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]+"), "-").ifBlank { "bundle.reseam" }
        return File(bundleDirectory, safeName)
    }

    private suspend fun downloadInto(target: File, openInput: () -> InputStream): File = withContext(Dispatchers.IO) {
        openInput().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target
    }

    private suspend fun validate(file: File, source: String): BundleImportResult =
        try {
            backend.validateBundle(file.absolutePath, source)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
}
