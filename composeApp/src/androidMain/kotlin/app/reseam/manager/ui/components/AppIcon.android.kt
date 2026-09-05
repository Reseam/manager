package app.reseam.manager.ui.components

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

@Composable
actual fun rememberPackageIcon(packageName: String?): Painter? {
    if (packageName == null) return null
    val packageManager = LocalContext.current.packageManager
    return remember(packageName) {
        try {
            BitmapPainter(packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap())
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
