package app.reseam.manager.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reseam.manager.UpdateReason
import app.reseam.manager.UpdateStatus
import app.reseam.manager.data.ResolveProgress
import app.reseam.manager.resources.*
import app.reseam.manager.ui.ShownUpdate
import app.reseam.manager.ui.updateAction
import app.reseam.manager.ui.components.AppCard
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.downloadProgress
import org.jetbrains.compose.resources.stringResource

/** A patched app an update would patch again: why, or how far the update is. */
@Composable
fun AppUpdateCard(update: ShownUpdate, onUpdate: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    AppCard(
        app = update.look,
        supporting = supporting(update),
        onClick = onOpen,
        modifier = modifier,
        selected = selected,
        trailing = {
            if (update.startable) {
                Button(stringResource(updateAction(update)), onUpdate, size = ButtonSize.Medium)
            } else {
                Spinner()
            }
        },
    )
}

@Composable
private fun supporting(update: ShownUpdate): String = when (val status = update.status) {
    null -> when (val reason = update.update.reason) {
        UpdateReason.Patches -> stringResource(Res.string.home_update_patches)
        is UpdateReason.Version -> stringResource(Res.string.home_update_version, reason.versionName)
        is UpdateReason.Original -> stringResource(Res.string.home_update_original, reason.versionName)
    }
    UpdateStatus.Waiting -> stringResource(Res.string.home_update_waiting)
    is UpdateStatus.Preparing -> when (val progress = status.progress) {
        ResolveProgress.Preparing -> stringResource(Res.string.run_preparing)
        is ResolveProgress.Downloading -> downloadProgress(progress.transfer)
    }
    UpdateStatus.Patching -> stringResource(Res.string.home_update_patching)
    UpdateStatus.Installing -> stringResource(Res.string.artifact_installing)
    is UpdateStatus.Failed -> status.error.describe()
}
