package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme
import kotlin.math.roundToInt

data class Segment<T>(val value: T, val label: String, val icon: ImageVector? = null)

@Composable
fun <T> SegmentedControl(
    segments: List<Segment<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val shape = ReseamTheme.shapes.medium
    val selectedIndex = segments.indexOfFirst { it.value == selected }
    val position = animateFloatAsState(selectedIndex.coerceAtLeast(0).toFloat(), motion.tweenBase(), label = "segment-indicator")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.surfaceSunken, shape)
            .border(1.dp, colors.border, shape)
            .padding(4.dp),
    ) {
        if (selectedIndex >= 0) {
            Box(
                Modifier
                    .fillMaxWidth(1f / segments.size)
                    .fillMaxHeight()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative((position.value * placeable.width).roundToInt(), 0)
                        }
                    }
                    .background(colors.primaryFaint, ReseamTheme.shapes.small),
            )
        }
        Row(Modifier.fillMaxSize().selectableGroup()) {
            segments.forEach { segment ->
                val active = segment.value == selected
                val tint by animateColorAsState(if (active) colors.primary else colors.mutedForeground, motion.tweenBase(), label = "segment-text")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(ReseamTheme.shapes.small)
                        .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(segment.value) }),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (segment.icon != null) Icon(segment.icon, null, tint = tint, modifier = Modifier.size(18.dp))
                    Text(segment.label, style = ReseamTheme.typography.captionMedium, color = tint)
                }
            }
        }
    }
}

@Composable
fun <T> ChoiceRow(
    choices: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
) {
    SegmentedControl(segments = choices.map { Segment(it, label(it)) }, selected = selected, onSelect = onSelect, modifier = modifier)
}
