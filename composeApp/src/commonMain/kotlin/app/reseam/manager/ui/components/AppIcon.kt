package app.reseam.manager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

/** A captured archive icon, or the installed package icon when no archive icon was supplied. */
@Composable
expect fun rememberAppIcon(packageName: String?, iconPath: String?): Painter?

private val TileHues = listOf(
    Color(0xFF6BC58E), Color(0xFF4F86FF), Color(0xFFE9B860), Color(0xFFD97A8C),
    Color(0xFF8B7CF6), Color(0xFF42B8C8), Color(0xFFE08A4E), Color(0xFF9CC85A),
)

@Composable
fun AppIcon(name: String, packageName: String?, modifier: Modifier = Modifier, size: Dp = 44.dp, iconPath: String? = null) {
    val shape = RoundedCornerShape(size / 4)
    val painter = rememberAppIcon(packageName, iconPath)
    if (painter != null) {
        Image(painter, contentDescription = null, modifier = modifier.size(size).clip(shape))
        return
    }
    val hue = TileHues[(packageName ?: name).hashCode().mod(TileHues.size)]
    Box(
        modifier = modifier.size(size).background(hue.copy(alpha = 0.22f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "?",
            style = ReseamTheme.typography.title.copy(fontWeight = FontWeight.Bold),
            color = hue,
        )
    }
}
