package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

enum class ButtonVariant { Primary, Ghost, Subtle, Danger }

enum class ButtonSize(val height: Dp, val horizontalPadding: Dp, val radius: Dp, val iconSize: Dp) {
    Small(36.dp, 14.dp, 10.dp, 16.dp),
    Medium(44.dp, 18.dp, 12.dp, 18.dp),
    Large(52.dp, 22.dp, 14.dp, 20.dp),
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = ReseamTheme.colors
    val shape = RoundedCornerShape(size.radius)
    val interaction = remember { MutableInteractionSource() }
    val background = when (variant) {
        ButtonVariant.Primary -> colors.primary
        ButtonVariant.Subtle -> colors.mutedElevated
        ButtonVariant.Ghost, ButtonVariant.Danger -> Color.Transparent
    }
    val foreground = when (variant) {
        ButtonVariant.Primary -> colors.onPrimary
        ButtonVariant.Danger -> colors.dangerForeground
        else -> colors.foreground
    }
    val outline = when (variant) {
        ButtonVariant.Ghost -> colors.border
        ButtonVariant.Danger -> colors.dangerHairline
        else -> null
    }
    val textStyle = if (size == ButtonSize.Small) ReseamTheme.typography.captionMedium else ReseamTheme.typography.bodyMedium
    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .alpha(if (enabled) 1f else 0.5f)
            .pressScale(interaction)
            .clip(shape)
            .background(background)
            .then(if (outline != null) Modifier.border(1.dp, outline, shape) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = size.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides foreground, LocalTextStyle provides textStyle) {
            if (icon != null) Icon(icon, null, modifier = Modifier.size(size.iconSize))
            content()
        }
    }
}

@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = ReseamTheme.colors.mutedForeground,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(CircleShape)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val width = if (small) 40.dp else 52.dp
    val height = if (small) 24.dp else 32.dp
    val knob = height - 8.dp
    val track by animateColorAsState(if (checked) colors.primary else colors.mutedElevated, motion.tweenBase(), label = "track")
    val knobColor by animateColorAsState(if (checked) colors.onPrimary else colors.mutedForeground, motion.tweenBase(), label = "knob")
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(width, height)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(track)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(knob).background(knobColor, CircleShape))
    }
}
