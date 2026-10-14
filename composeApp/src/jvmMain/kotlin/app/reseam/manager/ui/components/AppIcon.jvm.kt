package app.reseam.manager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

/** Decoded icons stay cached so shared element transitions start with the painter already loaded. */
private const val MaxCachedIcons = 96
private val iconCache = object : LinkedHashMap<String, BitmapPainter>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BitmapPainter>) = size > MaxCachedIcons
}

private fun cachedIcon(key: String): BitmapPainter? = synchronized(iconCache) { iconCache[key] }

@Composable
actual fun rememberAppIcon(packageName: String, iconPath: String?): Painter? {
    val key = iconPath ?: return null
    cachedIcon(key)?.let { return it }
    val painter by produceState<Painter?>(null, key) {
        value = withContext(Dispatchers.IO) { File(key).takeIf(File::exists)?.readBytes() }
            ?.let { bytes -> BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap()) }
            ?.also { painter -> synchronized(iconCache) { iconCache[key] = painter } }
    }
    return painter
}
