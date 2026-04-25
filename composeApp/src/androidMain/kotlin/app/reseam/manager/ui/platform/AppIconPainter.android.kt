package app.reseam.manager.ui.platform

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

@Composable
actual fun rememberPackageAppIconPainter(packageName: String?): Painter? {
    if (packageName.isNullOrBlank()) return null
    val context = LocalContext.current.applicationContext
    return remember(packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            BitmapPainter(drawable.toBitmap().asImageBitmap())
        }.getOrElse { error ->
            if (error is PackageManager.NameNotFoundException) null else null
        }
    }
}
