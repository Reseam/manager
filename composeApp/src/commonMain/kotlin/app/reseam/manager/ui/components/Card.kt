package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
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
fun Card(
    modifier: Modifier = Modifier,
    background: Color = ReseamTheme.colors.surface,
    borderColor: Color = ReseamTheme.colors.border,
    shape: RoundedCornerShape = ReseamTheme.shapes.card,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    val motion = ReseamTheme.motion
    val fill by animateColorAsState(background, motion.tweenBase(), label = "card-fill")
    val outline by animateColorAsState(borderColor, motion.tweenBase(), label = "card-outline")
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction, pressedScale = 0.985f) else Modifier)
            .clip(shape)
            .background(fill)
            .border(1.dp, outline, shape)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, enabled = enabled, onClick = onClick) else Modifier)
            .padding(contentPadding),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = ReseamTheme.colors.mutedElevated,
    tint: Color = ReseamTheme.colors.foreground,
) {
    Box(
        modifier = modifier.size(size).background(background, RoundedCornerShape(size / 3.6f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
    }
}
