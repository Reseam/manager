package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import app.reseam.manager.data.ReleaseMedia
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.MediaTile

@Composable
actual fun Video(media: ReleaseMedia, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    MediaTile(Icons.Play, media.name, { uriHandler.openUri(media.url) }, modifier)
}
