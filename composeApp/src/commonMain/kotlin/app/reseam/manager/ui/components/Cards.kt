package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RsCard(
    modifier: Modifier = Modifier,
    background: Color = ReseamTheme.colors.card,
    borderColor: Color? = ReseamTheme.colors.divider,
    cornerRadius: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    val motion = ReseamTheme.motion
    val shape = RoundedCornerShape(cornerRadius)
    val animBackground by animateColorAsState(
        targetValue = background,
        animationSpec = tween(motion.durationBase, easing = motion.easeOut),
        label = "rs-card-bg",
    )
    val animBorder by animateColorAsState(
        targetValue = borderColor ?: Color.Transparent,
        animationSpec = tween(motion.durationBase, easing = motion.easeOut),
        label = "rs-card-border",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressableModifier = if (onClick != null) {
        Modifier
            .rsPressScale(interactionSource, pressedScale = 0.985f, enabled = enabled)
            .clip(shape)
            .background(animBackground)
            .let { if (borderColor != null) it.border(1.dp, animBorder, shape) else it }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
    } else {
        Modifier
            .clip(shape)
            .background(animBackground)
            .let { if (borderColor != null) it.border(1.dp, animBorder, shape) else it }
    }
    Box(
        modifier = modifier
            .then(pressableModifier)
            .padding(contentPadding),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

@Composable
fun RsIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = 12.dp,
    background: Color = ReseamTheme.colors.mutedElevated,
    tint: Color = ReseamTheme.colors.foreground,
    iconSize: Dp = ReseamTheme.dimens.iconStandard,
    contentDescription: String? = null,
) {
    val motion = ReseamTheme.motion
    val animBackground by animateColorAsState(
        targetValue = background,
        animationSpec = tween(motion.durationBase, easing = motion.easeOut),
        label = "rs-icon-tile-bg",
    )
    val animTint by animateColorAsState(
        targetValue = tint,
        animationSpec = tween(motion.durationBase, easing = motion.easeOut),
        label = "rs-icon-tile-tint",
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(animBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = animTint,
            modifier = Modifier.size(iconSize),
        )
    }
}
