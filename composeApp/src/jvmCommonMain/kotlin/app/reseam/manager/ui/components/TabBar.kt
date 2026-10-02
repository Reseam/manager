package app.reseam.manager.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

/** Navigation between related views, with an underline marking the active view. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> TabBar(
    tabs: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    SecondaryScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.value == selected }.coerceAtLeast(0),
        modifier = modifier,
        edgePadding = 0.dp,
        containerColor = colors.background,
        contentColor = colors.primary,
        divider = { Divider() },
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab.value == selected,
                onClick = { onSelect(tab.value) },
                selectedContentColor = colors.primary,
                unselectedContentColor = colors.mutedForeground,
                text = { Text(tab.label, style = ReseamTheme.typography.captionMedium) },
            )
        }
    }
}
