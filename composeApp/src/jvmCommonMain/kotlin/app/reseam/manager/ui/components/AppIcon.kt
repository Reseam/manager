package app.reseam.manager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp

@Composable
expect fun rememberAppIcon(packageName: String, iconPath: String?): Painter?

data class AppLook(val name: String, val packageName: String, val iconPath: String? = null)

@Composable
fun AppIcon(app: AppLook, size: Dp, modifier: Modifier = Modifier) {
    val painter = rememberAppIcon(app.packageName, app.iconPath)
    if (painter != null) {
        Image(painter, contentDescription = null, modifier = modifier.size(size).clip(CircleShape))
        return
    }
    Box(modifier.size(size).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Package, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(size / 2))
    }
}
