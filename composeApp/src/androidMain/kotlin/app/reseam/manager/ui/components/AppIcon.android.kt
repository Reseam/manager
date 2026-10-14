package app.reseam.manager.ui.components

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decoded icons stay cached so shared element transitions start with the painter already loaded. */
private const val MaxCachedIcons = 96
private val iconCache = object : LinkedHashMap<String, BitmapPainter>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BitmapPainter>) = size > MaxCachedIcons
}

private fun cachedIcon(key: String): BitmapPainter? = synchronized(iconCache) { iconCache[key] }

@Composable
actual fun rememberAppIcon(packageName: String, iconPath: String?): Painter? {
    val key = iconPath ?: "installed:$packageName"
    cachedIcon(key)?.let { return it }
    val packageManager = LocalContext.current.packageManager
    val painter by produceState<Painter?>(null, key) {
        value = withContext(Dispatchers.IO) {
            if (iconPath != null) {
                BitmapFactory.decodeFile(iconPath)
            } else {
                try {
                    packageManager.getApplicationIcon(packageName).toBitmap()
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
        }
            ?.let { BitmapPainter(it.asImageBitmap()) }
            ?.also { painter -> synchronized(iconCache) { iconCache[key] = painter } }
    }
    return painter
}
