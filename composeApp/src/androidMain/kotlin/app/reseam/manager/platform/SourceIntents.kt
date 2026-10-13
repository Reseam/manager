package app.reseam.manager.platform

import android.content.Intent
import app.reseam.manager.data.SourceInput
import io.github.vinceglb.filekit.PlatformFile

private const val AddLinkHost = "reseam.app"

private val AddLinkPaths = setOf("/add", "/add/")

/** A reseam.app/add link names the source to add; a viewed content URI is a .reseam file another app opened. */
fun Intent.sourceInput(): SourceInput? {
    if (action != Intent.ACTION_VIEW) return null
    val uri = data ?: return null
    return when (uri.scheme) {
        "https" -> uri.takeIf { it.host == AddLinkHost && it.path in AddLinkPaths }?.getQueryParameter("url")?.let(SourceInput::Link)
        "content" -> SourceInput.File(PlatformFile(uri))
        else -> null
    }
}
