package app.reseam.manager.ui

import androidx.compose.runtime.Composable
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.Icons
import org.jetbrains.compose.resources.stringResource

/** Why [update] stopped, before trying it again. */
@Composable
fun UpdateStoppedDialog(update: ShownUpdate, error: Exception, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        headline = stringResource(Res.string.update_stopped_title, update.update.app.name),
        body = error.describe(),
        icon = Icons.CircleAlert,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(Res.string.close), onDismiss),
            DialogAction(stringResource(Res.string.try_again), {
                onDismiss()
                onRetry()
            }),
        ),
    )
}
