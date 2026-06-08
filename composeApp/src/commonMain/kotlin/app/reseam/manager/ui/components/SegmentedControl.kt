package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

data class RsSegment<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
)

@Composable
fun <T> RsSegmentedControl(
    selected: T,
    segments: List<RsSegment<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val outerCorner = 12.dp
    val innerCorner = 9.dp
    val padding = 4.dp
    val gap = 2.dp
    val rowHeight = 34.dp
    val selectedIndex = segments.indexOfFirst { it.value == selected }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(outerCorner))
            .background(colors.cardElevated)
            .border(1.dp, colors.border, RoundedCornerShape(outerCorner))
            .padding(padding),
    ) {
        val totalWidth = maxWidth
        val segCount = segments.size.coerceAtLeast(1)
        val pillWidth = (totalWidth - gap * (segCount - 1)) / segCount
        val targetX = (pillWidth + gap) * selectedIndex
        val animX by animateDpAsState(
            targetValue = targetX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "rs-seg-indicator",
        )
        // Sliding indicator
        Box(
            modifier = Modifier
                .offset(x = animX)
                .width(pillWidth)
                .height(rowHeight)
                .clip(RoundedCornerShape(innerCorner))
                .background(colors.accent),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            segments.forEach { seg ->
                val isSelected = seg.value == selected
                val targetFg = if (isSelected) colors.foreground else colors.mutedForeground
                val animFg by animateColorAsState(
                    targetValue = targetFg,
                    animationSpec = tween(motion.durationBase, easing = motion.easeOut),
                    label = "rs-seg-fg",
                )
                val interaction = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(rowHeight)
                        .clip(RoundedCornerShape(innerCorner))
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelect(seg.value) },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    if (seg.icon != null) {
                        Icon(
                            imageVector = seg.icon,
                            contentDescription = null,
                            tint = animFg,
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                        )
                    }
                    Text(
                        text = seg.label,
                        style = ReseamTheme.typography.caption,
                        color = animFg,
                    )
                }
            }
        }
    }
}

