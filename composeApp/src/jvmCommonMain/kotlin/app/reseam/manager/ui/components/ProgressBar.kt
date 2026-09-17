package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.muted)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(6.dp).background(Brush.horizontalGradient(listOf(colors.primary, colors.primaryBright))))
    }
}
