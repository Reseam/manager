package app.reseam.manager.data.platform

import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.validateBundle
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.ui.model.BundleSummary
import java.io.File
import java.net.URL

class DesktopBundleImporter(
    private val backend: ReseamBackend,
    private val bundleDirectory: File = desktopDataFile("bundles").also { it.mkdirs() },
) : BundleImporter {
    override suspend fun importFromUrl(url: String): BundleSummary {
        val target = bundleFile(url.substringAfterLast('/').ifBlank { "bundle.reseam" })
        URL(url).openStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return validate(target, source = url)
    }

    override suspend fun importFromFile(path: String): BundleSummary {
        val source = File(path)
        require(source.isFile) { "Bundle file does not exist: $path" }
        val target = bundleFile(source.name)
        source.copyTo(target, overwrite = true)
        return validate(target, source = source.absolutePath)
    }

    private fun bundleFile(name: String): File {
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]+"), "-").ifBlank { "bundle.reseam" }
        return File(bundleDirectory, safeName)
    }

    private suspend fun validate(file: File, source: String): BundleSummary =
        try {
            backend.validateBundle(file.absolutePath, source)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
}
