package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

class RailDestination<T>(val value: T, val label: String, val icon: ImageVector)

/** Persistent navigation for windows wide enough to keep it beside the content. */
@Composable
fun <T> NavigationRail(
    destinations: List<RailDestination<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    Column(
        modifier = modifier.width(96.dp).fillMaxHeight().background(colors.surfaceSunken).padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LogoMark(size = 30.dp, modifier = Modifier.padding(bottom = 20.dp))
        destinations.forEach { destination ->
            RailItem(destination, destination.value == selected, onClick = { onSelect(destination.value) })
        }
    }
}

@Composable
private fun <T> RailItem(destination: RailDestination<T>, selected: Boolean, onClick: () -> Unit) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val indicator by animateColorAsState(if (selected) colors.primarySoft else colors.surfaceSunken, motion.tweenBase(), label = "rail-indicator")
    val tint by animateColorAsState(if (selected) colors.primary else colors.mutedForeground, motion.tweenBase(), label = "rail-tint")
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clip(ReseamTheme.shapes.medium)
            .clickable(interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(width = 56.dp, height = 32.dp).background(indicator, ReseamTheme.shapes.pill), contentAlignment = Alignment.Center) {
            Icon(destination.icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(destination.label, style = ReseamTheme.typography.chip, color = if (selected) colors.foreground else colors.mutedForeground)
    }
}
