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
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopBundleImporter(
    private val backend: ReseamBackend,
    private val bundleDirectory: File = desktopDataFile("bundles"),
) : BundleImporter {
    override suspend fun syncOfficial(apiBaseUrl: String, currentVersion: String?): BundleImportResult? {
        val indexJson = withContext(Dispatchers.IO) {
            URL(officialPatchesIndexUrl(apiBaseUrl)).openStream().bufferedReader().use { it.readText() }
        }
        val index = ReseamJson.codec.decodeFromString<OfficialPatchesIndex>(indexJson)
        val release = index.latestStableRelease() ?: error("Official bundle has no stable release")
        if (currentVersion != null && currentVersion == release.version) return null
        val target = bundleFile(release.downloadUrl.substringAfterLast('/'))
        withContext(Dispatchers.IO) {
            URL(release.downloadUrl).openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return validate(target, source = release.downloadUrl, autoTrust = true).markOfficial(release)
    }

    override suspend fun importFromUrl(url: String): BundleImportResult {
        val target = bundleFile(url.substringAfterLast('/'))
        withContext(Dispatchers.IO) {
            URL(url).openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return validate(target, source = url)
    }

    override suspend fun importFromFile(path: String): BundleImportResult {
        val source = File(path)
        check(source.isFile) { "Bundle file does not exist: $path" }
        val target = bundleFile(source.name)
        withContext(Dispatchers.IO) { source.copyTo(target, overwrite = true) }
        return validate(target, source = source.absolutePath)
    }

    private fun bundleFile(name: String): File {
        bundleDirectory.mkdirs()
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]+"), "-").ifBlank { "bundle.reseam" }
        return File(bundleDirectory, safeName)
    }

    private suspend fun validate(file: File, source: String, autoTrust: Boolean = false): BundleImportResult =
        try {
            backend.validateBundle(file.absolutePath, source, autoTrust = autoTrust)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
}
