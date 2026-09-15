package app.reseam.manager.ui.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reseam.manager.platform.textClipEntry
import app.reseam.manager.ui.theme.ReseamTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(ReseamTheme.colors.divider))
}

val ItemTextSpacing = 6.dp

/** A titled group of rows in one card; rows separate themselves with dividers. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ItemTextSpacing)) {
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
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(value) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(CopiedConfirmationMillis)
            copied = false
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Copy $label") {
                scope.launch {
                    clipboard.setClipEntry(textClipEntry(label, value))
                    copied = true
                }
            }
            .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mono) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ItemTextSpacing)) {
                Text(label, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
                Text(value, style = ReseamTheme.typography.monoSmall, color = colors.foreground)
            }
        } else {
            Text(label, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
            Text(value, style = ReseamTheme.typography.captionMedium, color = colors.foreground, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Crossfade(copied, animationSpec = ReseamTheme.motion.tweenFast(), label = "copied") { done ->
            Icon(
                imageVector = if (done) Icons.Check else Icons.Copy,
                contentDescription = if (done) "Copied" else null,
                tint = if (done) colors.primary else colors.mutedForeground,
                modifier = Modifier.size(18.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private const val CopiedConfirmationMillis = 1_500L

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
