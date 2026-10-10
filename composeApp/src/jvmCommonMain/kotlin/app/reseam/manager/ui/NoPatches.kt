package app.reseam.manager.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.StateMessage
import org.jetbrains.compose.resources.stringResource

/** Bundles only sync once Manager is current, so a pending update or a failed sync leaves nothing to patch. */
@Composable
fun NoPatches(updateRequired: Boolean, failure: Throwable?, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    when {
        updateRequired -> StateMessage(Icons.Download, stringResource(Res.string.no_patches_update_title), stringResource(Res.string.no_patches_update_body), modifier)
        failure != null -> StateMessage(
            icon = Icons.Warning,
            title = stringResource(Res.string.no_patches_failed_title),
            body = failure.describe(),
            modifier = modifier,
            action = DialogAction(stringResource(Res.string.try_again), onRetry),
        )
        else -> Loading(modifier)
    }
}
