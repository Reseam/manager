package app.reseam.manager.ui.run

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ProgressRow
import app.reseam.manager.ui.components.ProgressState
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.rememberClipboardCopy
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.theme.extendedColors
import app.reseam.manager.ui.theme.extendedTypography
import app.reseam.sdk.PatchStatus
import org.jetbrains.compose.resources.stringResource

@Composable
fun RunPatches(state: RunState, modifier: Modifier) {
    var logOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = Space.md)) {
        item { SectionLabel(stringResource(Res.string.app_patches), Modifier.padding(horizontal = Layout.margin)) }
        items(state.queue, key = { it }) { reference ->
            ProgressRow(
                title = state.names[reference] ?: reference,
                state = when (state.statuses[reference]) {
                    PatchStatus.Applied -> ProgressState.Applied
                    is PatchStatus.Failed -> ProgressState.Failed
                    is PatchStatus.Skipped, is PatchStatus.Unmountable -> ProgressState.Skipped
                    null -> if (reference == state.current) ProgressState.Running else ProgressState.Pending
                },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { logOpen = !logOpen }.padding(horizontal = Layout.margin, vertical = Space.sm).padding(top = Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(stringResource(Res.string.run_log), Modifier.weight(1f))
                if (logOpen && state.log.isNotEmpty()) CopyLogButton(state.log)
                Chevron(expanded = logOpen)
            }
        }
        if (logOpen) runLog(state.log, Modifier.padding(horizontal = Layout.margin))
    }
}

fun LazyListScope.runLog(lines: List<LogLine>, modifier: Modifier = Modifier) {
    if (lines.isEmpty()) {
        item { Text(stringResource(Res.string.run_log_empty), modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    items(lines) { line ->
        Text(
            text = line.text,
            modifier = modifier.padding(vertical = Space.xxs),
            style = MaterialTheme.extendedTypography.monoExtraSmall,
            color = MaterialTheme.extendedColors.warning,
        )
    }
}

@Composable
fun CopyLogButton(lines: List<LogLine>) {
    val copy = rememberClipboardCopy()
    val label = stringResource(Res.string.run_log)
    IconButton(Icons.Copy, stringResource(Res.string.run_copy_log), onClick = { copy(label) { lines.joinToString("\n") { it.text } } })
}
