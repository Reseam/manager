package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

enum class RsChipVariant { Default, Primary, SolidPrimary, Amber }

@Composable
fun RsChip(
    text: String,
    modifier: Modifier = Modifier,
    variant: RsChipVariant = RsChipVariant.Default,
    leading: @Composable (() -> Unit)? = null,
) {
    val colors = ReseamTheme.colors
    val (background, foreground, borderColor) = when (variant) {
        RsChipVariant.Default -> Triple(Color(0xFF1F1F1F), Color(0xFFD4D4D4), colors.borderStrong)
        RsChipVariant.Primary -> Triple(colors.primaryFaint, colors.primary, colors.primaryHairline)
        RsChipVariant.SolidPrimary -> Triple(colors.primary, colors.primaryForeground, colors.primary)
        RsChipVariant.Amber -> Triple(colors.warningSoft, colors.warningForeground, colors.warningHairline)
    }
    Row(
        modifier = modifier
            .background(background, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (leading != null) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides foreground,
            ) { leading() }
        }
        Text(
            text = text,
            style = ReseamTheme.typography.captionSmall,
            color = foreground,
        )
    }
}
