package app.reseam.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.reseam.manager.ui.theme.ReseamTheme

/** A bottom sheet on phones and a centered dialog everywhere wider. Content lays out the same in both. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = ReseamTheme.colors
    if (ReseamTheme.layout.compact) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = ReseamTheme.shapes.sheet,
            containerColor = colors.surface,
            contentColor = colors.foreground,
            scrimColor = colors.scrim,
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp), content = content)
        }
        return
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.padding(24.dp).widthIn(max = 480.dp).fillMaxWidth(),
            background = colors.surface,
            borderColor = colors.borderStrong,
            shape = ReseamTheme.shapes.dialog,
            contentPadding = PaddingValues(24.dp),
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SheetHeader(title: String, body: String? = null, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = ReseamTheme.typography.title, color = colors.foreground)
        if (body != null) Text(body, style = ReseamTheme.typography.bodySmall, color = colors.mutedForeground)
    }
}
