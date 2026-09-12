package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSunken, shape)
            .border(1.dp, colors.border, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        segments.forEach { segment ->
            val active = segment.value == selected
            val fill by animateColorAsState(if (active) colors.primaryFaint else colors.surfaceSunken, motion.tweenBase(), label = "segment")
            val tint by animateColorAsState(if (active) colors.primary else colors.mutedForeground, motion.tweenBase(), label = "segment-text")
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(ReseamTheme.shapes.small)
                    .background(fill)
                    .clickable { onSelect(segment.value) },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (segment.icon != null) Icon(segment.icon, null, tint = tint, modifier = Modifier.size(18.dp))
                Text(segment.label, style = ReseamTheme.typography.captionMedium, color = tint)
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
