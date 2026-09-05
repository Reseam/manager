package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

enum class ChipVariant { Neutral, Primary, SolidPrimary, Warning }

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, variant: ChipVariant = ChipVariant.Neutral) {
    val colors = ReseamTheme.colors
    val (fill, outline, foreground) = when (variant) {
        ChipVariant.Neutral -> Triple(colors.mutedElevated, colors.borderStrong, colors.mutedForeground)
        ChipVariant.Primary -> Triple(colors.primaryFaint, colors.primaryHairline, colors.primary)
        ChipVariant.SolidPrimary -> Triple(colors.primary, colors.primary, colors.onPrimary)
        ChipVariant.Warning -> Triple(colors.warningSoft, colors.warningHairline, colors.warningForeground)
    }
    Text(
        text = text,
        style = ReseamTheme.typography.captionMedium,
        color = foreground,
        modifier = modifier
            .background(fill, CircleShape)
            .border(1.dp, outline, CircleShape)
            .padding(horizontal = 9.dp, vertical = 2.dp),
    )
}
