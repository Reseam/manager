package app.reseam.manager.ui

import androidx.compose.runtime.Composable
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.ConfirmDialog
import app.reseam.manager.ui.components.Icons
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReplaceDialog(appName: String, onReplace: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        headline = stringResource(Res.string.replace_title, appName),
        body = stringResource(Res.string.replace_body, appName),
        icon = Icons.Warning,
        confirm = stringResource(Res.string.replace_action),
        onConfirm = onReplace,
        onDismiss = onDismiss,
    )
}
