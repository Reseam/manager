package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardElevated)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEach { seg ->
            val isSelected = seg.value == selected
            val bg = if (isSelected) colors.accent else Color.Transparent
            val fg = if (isSelected) colors.foreground else colors.mutedForeground
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .clickable(onClick = { onSelect(seg.value) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                if (seg.icon != null) {
                    Icon(
                        imageVector = seg.icon,
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                    )
                }
                Text(
                    text = seg.label,
                    style = ReseamTheme.typography.caption,
                    color = fg,
                )
            }
        }
    }
}
