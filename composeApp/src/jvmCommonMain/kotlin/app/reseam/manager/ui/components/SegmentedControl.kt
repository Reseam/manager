package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

data class Segment<T>(val value: T, val label: String, val icon: ImageVector? = null)

private enum class SegmentSlot { Segments, Indicator }

/** Each segment is its label plus an equal share of the spare width, so the indicator sits the same distance from every label. */
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
    SubcomposeLayout(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colors.surfaceSunken, shape)
            .border(1.dp, colors.border, shape)
            .padding(4.dp)
            .selectableGroup(),
    ) { constraints ->
        val height = constraints.maxHeight
        val items = subcompose(SegmentSlot.Segments) {
            segments.forEach { segment ->
                val active = segment.value == selected
                val tint by animateColorAsState(if (active) colors.primary else colors.mutedForeground, motion.tweenBase(), label = "segment-text")
                Row(
                    modifier = Modifier
                        .clip(ReseamTheme.shapes.small)
                        .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(segment.value) })
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (segment.icon != null) Icon(segment.icon, null, tint = tint, modifier = Modifier.size(18.dp))
                    Text(segment.label, style = ReseamTheme.typography.captionMedium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val natural = items.map { it.maxIntrinsicWidth(height) }
        val spare = constraints.maxWidth - natural.sum()
        val widths = if (spare >= 0) natural.map { it + spare / items.size } else natural.map { it * constraints.maxWidth / natural.sum() }
        val offsets = widths.runningFold(0, Int::plus)
        val placeables = items.mapIndexed { index, item -> item.measure(Constraints.fixed(widths[index], height)) }
        val indicator = selectedIndex.takeIf { it >= 0 }?.let { index ->
            subcompose(SegmentSlot.Indicator) { SegmentIndicator(offsets[index].toDp(), widths[index].toDp()) }
                .single()
                .measure(Constraints.fixed(constraints.maxWidth, height))
        }
        layout(constraints.maxWidth, height) {
            indicator?.place(0, 0)
            placeables.forEachIndexed { index, placeable -> placeable.place(offsets[index], 0) }
        }
    }
}

@Composable
private fun SegmentIndicator(offset: Dp, width: Dp) {
    val motion = ReseamTheme.motion
    val x by animateDpAsState(offset, motion.tweenBase(), label = "segment-indicator-offset")
    val w by animateDpAsState(width, motion.tweenBase(), label = "segment-indicator-width")
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().background(ReseamTheme.colors.primaryFaint, ReseamTheme.shapes.small))
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
