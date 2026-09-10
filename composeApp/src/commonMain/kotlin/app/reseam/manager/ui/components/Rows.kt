package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(ReseamTheme.colors.divider))
}

/** A titled group of rows in one card; rows separate themselves with dividers. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                rows.forEachIndexed { index, row ->
                    row()
                    if (index < rows.lastIndex) Divider(Modifier.padding(start = 60.dp))
                }
            }
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {
        if (onClick != null) Icon(Icons.ChevronRight, null, tint = ReseamTheme.colors.subtleForeground, modifier = Modifier.size(20.dp))
    },
) {
    val colors = ReseamTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = colors.mutedForeground, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
            if (subtitle != null) {
                Text(subtitle, style = ReseamTheme.typography.caption, color = colors.mutedForeground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    val colors = ReseamTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
        Text(
            text = value,
            style = if (mono) ReseamTheme.typography.monoSmall else ReseamTheme.typography.captionMedium,
            color = colors.foreground,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Rows in a card, divider between each. */
@Composable
fun InfoCard(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column {
            rows.forEachIndexed { index, row ->
                row()
                if (index < rows.lastIndex) Divider()
            }
        }
    }
}
