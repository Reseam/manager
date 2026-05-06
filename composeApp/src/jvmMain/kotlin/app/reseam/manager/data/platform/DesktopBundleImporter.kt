package app.reseam.manager.data.platform

import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.BundleImportResult
import app.reseam.manager.domain.sources.OfficialPatchesIndex
import app.reseam.manager.domain.sources.markOfficial
import app.reseam.manager.domain.sources.officialPatchesIndexUrl
import app.reseam.manager.domain.sources.validateBundle
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.patcher.ReseamJson
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopBundleImporter(
    private val backend: ReseamBackend,
    private val bundleDirectory: File = desktopDataFile("bundles"),
) : BundleImporter {
    override suspend fun syncOfficial(apiBaseUrl: String, currentVersion: String?): BundleImportResult? {
        val indexJson = withContext(Dispatchers.IO) {
            timeBoundedConnection(URL(officialPatchesIndexUrl(apiBaseUrl))).use { input ->
                input.bufferedReader().readText()
            }
        }
        val index = ReseamJson.codec.decodeFromString<OfficialPatchesIndex>(indexJson)
        val release = requireNotNull(index.latestStableRelease()) { "Official bundle has no stable release" }
        if (currentVersion != null && currentVersion == release.version) return null
        return stageAndValidate(source = release.downloadUrl, autoTrust = true) {
            timeBoundedConnection(URL(release.downloadUrl))
        }.markOfficial(release)
    }

    override suspend fun importFromUrl(url: String): BundleImportResult =
        stageAndValidate(source = url) { timeBoundedConnection(URL(url)) }

    override suspend fun importFromFile(path: String): BundleImportResult {
        val source = File(path)
        check(source.isFile) { "Bundle file does not exist: $path" }
        return stageAndValidate(source = source.absolutePath) { source.inputStream() }
    }

    private suspend fun stageAndValidate(
        source: String,
        autoTrust: Boolean = false,
        openInput: () -> InputStream,
    ): BundleImportResult {
        val staged = stagingFile()
        return try {
            withContext(Dispatchers.IO) {
                openInput().use { input ->
                    staged.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val validated = backend.validateBundle(staged.absolutePath, source, autoTrust = autoTrust)
            val signer = requireNotNull(validated.summary.signerPublicKeyHex?.takeIf { it.isNotBlank() }) {
                "Validated bundle has no signer public key"
            }
            val finalFile = bundleFile("$signer.reseam")
            moveOrCopy(staged, finalFile)
            validated.copy(summary = validated.summary.copy(path = finalFile.absolutePath))
        } catch (error: Throwable) {
            staged.delete()
            throw error
        }
    }

    private fun stagingFile(): File {
        bundleDirectory.mkdirs()
        return File(bundleDirectory, ".staging-${UUID.randomUUID()}.reseam")
    }

    private fun bundleFile(name: String): File {
        bundleDirectory.mkdirs()
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]+"), "-").ifBlank { "bundle.reseam" }
        return File(bundleDirectory, safeName)
    }

    private fun moveOrCopy(source: File, target: File) {
        if (target.exists()) target.delete()
        if (source.renameTo(target)) return
        source.copyTo(target, overwrite = true)
        source.delete()
    }

    private fun timeBoundedConnection(url: URL): InputStream {
        val connection = url.openConnection().apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        return connection.getInputStream()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
    }
}
