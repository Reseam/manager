package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

/** Where a screen is rendered: alone, as the list beside a detail, or as that detail. */
enum class PaneRole { Single, List, Detail }

val LocalPaneRole = staticCompositionLocalOf { PaneRole.Single }

/** Grid columns for the space a screen actually has: a list pane is one column wide whatever the window. */
@Composable
fun paneGridColumns(): Int = if (LocalPaneRole.current == PaneRole.List) 1 else ReseamTheme.layout.gridColumns

/** A list pane at the layout's list width and the detail filling the rest, divided by a hairline. */
@Composable
fun TwoPane(list: @Composable () -> Unit, detail: @Composable () -> Unit) {
    val colors = ReseamTheme.colors
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(ReseamTheme.layout.listPaneWidth).fillMaxHeight()) { list() }
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.divider))
        Box(Modifier.weight(1f).fillMaxHeight()) { detail() }
    }
}

@Composable
fun DetailPlaceholder(icon: ImageVector, title: String, body: String) {
    val colors = ReseamTheme.colors
    Box(Modifier.fillMaxSize().padding(ReseamTheme.layout.pageMargin), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(icon, size = 64.dp, background = colors.muted, tint = colors.mutedForeground)
            Text(title, style = ReseamTheme.typography.titleSmall, color = colors.foreground, textAlign = TextAlign.Center)
            Text(body, style = ReseamTheme.typography.bodySmall, color = colors.mutedForeground, textAlign = TextAlign.Center)
        }
    }
}
