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

@Composable
actual fun rememberAppIcon(packageName: String?, iconPath: String?): Painter? {
    val packageManager = LocalContext.current.packageManager
    val painter by produceState<Painter?>(null, packageName, iconPath) {
        value = withContext(Dispatchers.IO) {
            val bitmap = when {
                iconPath != null -> BitmapFactory.decodeFile(iconPath)
                packageName != null -> try {
                    packageManager.getApplicationIcon(packageName).toBitmap()
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
                else -> null
            }
            bitmap?.let { BitmapPainter(it.asImageBitmap()) }
        }
    }
    return painter
}
