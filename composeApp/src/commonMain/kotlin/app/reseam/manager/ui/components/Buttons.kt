package app.reseam.manager.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
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
    val background = when (variant) {
        RsButtonVariant.Primary -> colors.primary
        RsButtonVariant.Ghost -> Color.Transparent
        RsButtonVariant.Subtle -> colors.mutedElevated
        RsButtonVariant.Danger -> Color.Transparent
    }
    val foreground = when (variant) {
        RsButtonVariant.Primary -> colors.primaryForeground
        RsButtonVariant.Ghost -> colors.foreground
        RsButtonVariant.Subtle -> colors.foreground
        RsButtonVariant.Danger -> colors.destructiveForeground
    }
    val border = when (variant) {
        RsButtonVariant.Ghost -> BorderStroke(1.dp, colors.border)
        RsButtonVariant.Danger -> BorderStroke(1.dp, Color(0xFF3A1A1A))
        else -> null
    }
    val labelStyle = when (size) {
        RsButtonSize.Small -> ReseamTheme.typography.caption
        RsButtonSize.Medium -> ReseamTheme.typography.body
        RsButtonSize.Large -> ReseamTheme.typography.titleSmall
    }

    val baseModifier = modifier
        .let { if (fullWidth) it.fillMaxWidth() else it }
        .heightIn(min = height)
        .clip(RoundedCornerShape(cornerRadius))
        .background(background)
        .let { m -> if (border != null) m.border(border, RoundedCornerShape(cornerRadius)) else m }
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = horizontalPadding)
        .alpha(if (enabled) 1f else 0.5f)

    Row(
        modifier = baseModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides foreground,
        ) {
            androidx.compose.material3.ProvideTextStyle(value = labelStyle.copy(color = foreground)) {
                content()
            }
        }
    }
}

@Composable
fun RsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    tint: Color = ReseamTheme.colors.mutedForeground,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides tint,
        ) {
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
    val small = size == RsButtonSize.Small
    val w = if (small) 36.dp else 46.dp
    val h = if (small) 22.dp else 28.dp
    val knob = h - 6.dp
    val travel = w - knob - 6.dp
    val targetOffset = if (checked) travel else 0.dp
    val offset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(280, easing = ReseamTheme.motion.easeSpring),
        label = "rs-toggle-knob",
    )
    val track = if (checked) colors.primary else Color(0xFF2A2A2A)
    val knobColor = if (checked) colors.primaryForeground else Color(0xFFD4D4D4)

    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width = w, height = h)
            .clip(CircleShape)
            .background(track)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(3.dp)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = offset)
                .size(knob)
                .clip(CircleShape)
                .background(knobColor),
        )
    }
}

@Suppress("unused")
@Composable
fun RsTextLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ReseamTheme.colors.mutedForeground,
) {
    Text(
        text = text,
        style = ReseamTheme.typography.label,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
