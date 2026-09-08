package app.reseam.manager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.File

@Composable
actual fun rememberAppIcon(packageName: String?, iconPath: String?): Painter? {
    val painter by produceState<Painter?>(null, iconPath) {
        value = iconPath
            ?.let { path -> withContext(Dispatchers.IO) { File(path).takeIf(File::exists)?.readBytes() } }
            ?.let { BitmapPainter(Image.makeFromEncoded(it).toComposeImageBitmap()) }
    }
    return painter
}
