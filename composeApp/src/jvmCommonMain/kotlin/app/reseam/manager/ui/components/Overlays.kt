package app.reseam.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reseam.manager.resources.*
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

data class DialogAction(val label: String, val onClick: () -> Unit, val destructive: Boolean = false, val enabled: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dialog(
    headline: String,
    actions: List<DialogAction>,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    body: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(Modifier.widthIn(min = 280.dp, max = 560.dp), shape = RoundedCornerShape(Radius.xl), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(Space.xxl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.lg)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(Sizes.iconMd))
                Text(headline, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                if (body != null) Text(body, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                content()
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                    horizontalArrangement = Arrangement.spacedBy(Space.sm, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    actions.forEach { action ->
                        Button(action.label, action.onClick, style = ButtonStyle.Text, enabled = action.enabled, destructive = action.destructive)
                    }
                }
            }
        }
    }
}

/** Asks before a destructive [confirm]; either choice closes it through [onDismiss]. */
@Composable
fun ConfirmDialog(
    headline: String,
    body: String,
    icon: ImageVector,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismiss: String = stringResource(Res.string.cancel),
) {
    Dialog(
        headline = headline,
        body = body,
        icon = icon,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(dismiss, onDismiss),
            DialogAction(confirm, {
                onConfirm()
                onDismiss()
            }, destructive = true),
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Layout.margin, end = Layout.margin, bottom = Space.xxl)
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            content()
        }
    }
}
