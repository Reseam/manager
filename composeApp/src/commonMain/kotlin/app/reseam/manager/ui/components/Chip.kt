package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val motion = ReseamTheme.motion
    val (background, foreground, borderColor) = when (variant) {
        RsChipVariant.Default -> Triple(colors.accent, colors.foreground, colors.borderStrong)
        RsChipVariant.Primary -> Triple(colors.primaryFaint, colors.primary, colors.primaryHairline)
        RsChipVariant.SolidPrimary -> Triple(colors.primary, colors.primaryForeground, colors.primary)
        RsChipVariant.Amber -> Triple(colors.warningSoft, colors.warningForeground, colors.warningHairline)
    }
    val animBackground by animateColorAsState(
        targetValue = background,
        animationSpec = tween(motion.durationBase, easing = motion.easeOut),
        label = "rs-chip-bg",
    )
    val animForeground by animateColorAsState(
        targetValue = foreground,
        animationSpec = tween(motion.durationBase, easing = motion.easeOut),
        label = "rs-chip-fg",
    )
    val animBorder by animateColorAsState(
        targetValue = borderColor,
        animationSpec = tween(motion.durationBase, easing = motion.easeOut),
        label = "rs-chip-border",
    )
    Row(
        modifier = modifier
            .background(animBackground, CircleShape)
            .border(1.dp, animBorder, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (leading != null) {
            CompositionLocalProvider(LocalContentColor provides animForeground) { leading() }
        }
        Text(
            text = text,
            style = ReseamTheme.typography.captionSmall,
            color = animForeground,
        )
    }
}
