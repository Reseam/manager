package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

enum class RsButtonVariant { Primary, Ghost, Subtle, Danger }

enum class RsButtonSize { Small, Medium, Large }

@Composable
fun RsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RsButtonVariant = RsButtonVariant.Primary,
    size: RsButtonSize = RsButtonSize.Medium,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val height = when (size) {
        RsButtonSize.Small -> 32.dp
        RsButtonSize.Medium -> 40.dp
        RsButtonSize.Large -> 48.dp
    }
    val horizontalPadding = when (size) {
        RsButtonSize.Small -> 12.dp
        RsButtonSize.Medium -> 16.dp
        RsButtonSize.Large -> 20.dp
    }
    val cornerRadius = when (size) {
        RsButtonSize.Small -> 10.dp
        RsButtonSize.Medium -> 12.dp
        RsButtonSize.Large -> 14.dp
    }
    val targetBackground = when (variant) {
        RsButtonVariant.Primary -> colors.primary
        RsButtonVariant.Ghost -> Color.Transparent
        RsButtonVariant.Subtle -> colors.mutedElevated
        RsButtonVariant.Danger -> Color.Transparent
    }
    val targetForeground = when (variant) {
        RsButtonVariant.Primary -> colors.primaryForeground
        RsButtonVariant.Ghost -> colors.foreground
        RsButtonVariant.Subtle -> colors.foreground
        RsButtonVariant.Danger -> colors.destructiveForeground
    }
    val border = when (variant) {
        RsButtonVariant.Ghost -> BorderStroke(1.dp, colors.border)
        RsButtonVariant.Danger -> BorderStroke(1.dp, colors.destructiveHairline)
        else -> null
    }
    val labelStyle = when (size) {
        RsButtonSize.Small -> ReseamTheme.typography.caption
        RsButtonSize.Medium -> ReseamTheme.typography.body
        RsButtonSize.Large -> ReseamTheme.typography.titleSmall
    }

    val animBackground by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(motion.durationFast, easing = motion.easeOut),
        label = "rs-btn-bg",
    )
    val animForeground by animateColorAsState(
        targetValue = targetForeground,
        animationSpec = tween(motion.durationFast, easing = motion.easeOut),
        label = "rs-btn-fg",
    )
    val animAlpha by animateColorAsState(
        targetValue = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
        animationSpec = tween(motion.durationFast, easing = motion.easeOut),
        label = "rs-btn-alpha",
    )

    val interactionSource = remember { MutableInteractionSource() }

    val baseModifier = modifier
        .let { if (fullWidth) it.fillMaxWidth() else it }
        .heightIn(min = height)
        .rsPressScale(interactionSource, enabled = enabled)
        .clip(RoundedCornerShape(cornerRadius))
        .background(animBackground)
        .let { m -> if (border != null) m.border(border, RoundedCornerShape(cornerRadius)) else m }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
        )
        .padding(horizontal = horizontalPadding)
        .alpha(animAlpha.alpha)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        CompositionLocalProvider(LocalContentColor provides animForeground) {
            ProvideTextStyle(labelStyle.copy(color = animForeground)) {
                content()
            }
        }
    }
}

@Composable
fun RsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = ReseamTheme.colors.mutedForeground,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animTint by animateColorAsState(
        targetValue = tint,
        animationSpec = tween(ReseamTheme.motion.durationFast, easing = ReseamTheme.motion.easeOut),
        label = "rs-icon-btn-tint",
    )
    Box(
        modifier = modifier
            .size(size)
            .rsPressScale(interactionSource, pressedScale = 0.92f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides animTint) {
            content()
        }
    }
}

@Composable
fun RsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: RsButtonSize = RsButtonSize.Medium,
    enabled: Boolean = true,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val small = size == RsButtonSize.Small
    val w = if (small) 36.dp else 46.dp
    val h = if (small) 22.dp else 28.dp
    val knob = h - 6.dp
    val travel = w - knob - 6.dp
    val targetOffset = if (checked) travel else 0.dp
    val offset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "rs-toggle-knob",
    )
    val track by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.borderStrong,
        animationSpec = tween(motion.durationBase, easing = motion.easeInOut),
        label = "rs-toggle-track",
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) colors.primaryForeground else colors.foreground,
        animationSpec = tween(motion.durationBase, easing = motion.easeInOut),
        label = "rs-toggle-knob-color",
    )
    val animAlpha by animateColorAsState(
        targetValue = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
        animationSpec = tween(motion.durationFast, easing = motion.easeOut),
        label = "rs-toggle-alpha",
    )

    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width = w, height = h)
            .rsPressScale(interaction, pressedScale = 0.94f, enabled = enabled)
            .clip(CircleShape)
            .background(track)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(3.dp)
            .alpha(animAlpha.alpha),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(knob)
                .clip(CircleShape)
                .background(knobColor),
        )
    }
}
