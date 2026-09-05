package app.reseam.manager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = ReseamTheme.colors
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
}
