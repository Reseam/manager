package app.reseam.manager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import app.reseam.manager.platform.textClipEntry
import app.reseam.manager.sdk.LogLevel
import app.reseam.manager.ui.theme.ReseamTheme
import kotlinx.coroutines.launch

data class LogLine(val level: LogLevel, val patch: String?, val message: String)

@Composable
fun LogDrawer(lines: List<LogLine>, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            background = colors.surfaceElevated,
            shape = ReseamTheme.shapes.medium,
            onClick = { open = !open },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Log, null, tint = colors.mutedForeground, modifier = Modifier.size(18.dp))
                Text(
                    text = if (open) "Hide log" else "Show log",
                    style = ReseamTheme.typography.caption,
                    color = colors.mutedForeground,
                    modifier = Modifier.weight(1f),
                )
                Text("${lines.size}", style = ReseamTheme.typography.captionSmall, color = colors.subtleForeground)
                Icon(Icons.ChevronDown, null, tint = colors.mutedForeground, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = if (open) 180f else 0f })
            }
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(motion.tweenBase()) + expandVertically(motion.tweenBase()),
            exit = fadeOut(motion.tweenFast()) + shrinkVertically(motion.tweenFast()),
        ) {
            LogPane(lines)
        }
    }
}

@Composable
private fun LogPane(lines: List<LogLine>) {
    val colors = ReseamTheme.colors
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }
    Card(background = colors.background, borderColor = colors.divider, shape = ReseamTheme.shapes.medium) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("LOG", style = ReseamTheme.typography.label, color = colors.mutedForeground, modifier = Modifier.weight(1f))
                Button(
                    onClick = { scope.launch { clipboard.setClipEntry(textClipEntry(lines.joinToString("\n") { it.format() })) } },
                    variant = ButtonVariant.Ghost,
                    size = ButtonSize.Small,
                ) { Text("Copy") }
            }
            Divider()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(lines) { line ->
                    Text(
                        text = line.format(),
                        style = ReseamTheme.typography.monoSmall,
                        color = when (line.level) {
                            LogLevel.Warn -> colors.warningForeground
                            LogLevel.Info -> colors.foreground
                            LogLevel.Debug -> colors.mutedForeground
                        },
                    )
                }
            }
        }
    }
}

private fun LogLine.format(): String = if (patch != null) "[$patch] $message" else message
