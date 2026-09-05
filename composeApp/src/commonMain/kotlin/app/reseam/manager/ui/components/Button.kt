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

enum class ButtonSize(val height: Dp, val horizontalPadding: Dp, val radius: Dp) {
    Small(32.dp, 12.dp, 10.dp),
    Medium(40.dp, 16.dp, 12.dp),
    Large(48.dp, 20.dp, 14.dp),
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
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
        CompositionLocalProvider(LocalContentColor provides foreground, LocalTextStyle provides textStyle) { content() }
    }
}

@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    tint: Color = ReseamTheme.colors.mutedForeground,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(CircleShape)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
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
    val width = if (small) 36.dp else 46.dp
    val height = if (small) 22.dp else 28.dp
    val knob = height - 6.dp
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
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(knob).background(knobColor, CircleShape))
    }
}
