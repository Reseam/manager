package app.reseam.manager.data.platform

import android.content.Context
import android.net.Uri
import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.validateBundle
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.ui.model.BundleSummary
import java.io.File
import java.net.URL

class AndroidBundleImporter(
    private val context: Context,
    private val backend: ReseamBackend,
) : BundleImporter {
    override suspend fun importFromUrl(url: String): BundleSummary {
        val target = bundleFile(url.substringAfterLast('/').ifBlank { "bundle.reseam" })
        URL(url).openStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return validate(target, source = url)
    }

    override suspend fun importFromFile(path: String): BundleSummary {
        val uri = runCatching { Uri.parse(path) }.getOrNull()
        val target = bundleFile(path.substringAfterLast('/').ifBlank { "bundle.reseam" })
        val inputStream = if (uri != null && uri.scheme != null) {
            context.contentResolver.openInputStream(uri)
        } else {
            File(path).inputStream()
        } ?: error("Could not open bundle: $path")

        inputStream.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return validate(target, source = path)
    }

    private fun bundleFile(name: String): File {
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]+"), "-").ifBlank { "bundle.reseam" }
        return File(context.filesDir, "reseam/bundles/$safeName").also { it.parentFile?.mkdirs() }
    }

    private suspend fun validate(file: File, source: String): BundleSummary =
        try {
            backend.validateBundle(file.absolutePath, source)
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
}
