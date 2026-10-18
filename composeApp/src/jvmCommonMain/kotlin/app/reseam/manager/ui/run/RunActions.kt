package app.reseam.manager.ui.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.resources.*
import app.reseam.manager.ui.ArtifactTask
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.deliverIcon
import app.reseam.manager.ui.deliverLabel
import app.reseam.manager.ui.shareLabel
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Space
import app.reseam.sdk.InstallMethod
import org.jetbrains.compose.resources.stringResource

@Composable
fun RunFooter(action: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(start = Layout.margin, end = Layout.margin, top = Space.sm, bottom = Space.sm)) { action() }
}

@Composable
fun RunAction(state: RunState, viewModel: RunViewModel, onClose: () -> Unit, onInstallInstead: () -> Unit) {
    val busy = viewModel.tasks.busy.collectAsStateWithLifecycle().value?.task
    val artifactKind = viewModel.artifactKind
    val modifier = Modifier.fillMaxWidth()
    when (state.stage) {
        is RunStage.Preparing, RunStage.Patching -> Button(stringResource(Res.string.cancel), onClose, modifier, style = ButtonStyle.Outlined, icon = Icons.Close)
        is RunStage.Failed -> Button(stringResource(Res.string.try_again), viewModel::retry, modifier, icon = Icons.Retry)
        is RunStage.Done -> Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            when {
                state.installedAs != null && artifactKind == ArtifactAction.Kind.Install -> Button(stringResource(Res.string.run_open), viewModel::open, modifier, icon = Icons.ExternalLink)
                viewModel.installMethod == InstallMethod.MOUNT -> Button(
                    label = stringResource(
                        when {
                            busy == ArtifactTask.Mount -> Res.string.run_mounting
                            state.leftOut.isNotEmpty() -> Res.string.run_mount_anyway
                            else -> Res.string.run_mount
                        },
                    ),
                    onClick = { viewModel.tasks.mount(viewModel.build) },
                    modifier = modifier,
                    icon = Icons.Layers,
                    enabled = busy == null,
                    loading = busy == ArtifactTask.Mount,
                )
                else -> Button(
                    label = artifactKind.deliverLabel(busy = busy == ArtifactTask.Deliver, anyway = state.leftOut.isNotEmpty()),
                    onClick = { viewModel.tasks.deliver(viewModel.build) },
                    modifier = modifier,
                    icon = artifactKind.deliverIcon,
                    enabled = busy == null,
                    loading = busy == ArtifactTask.Deliver,
                )
            }
            Button(
                label = artifactKind.shareLabel(),
                onClick = { viewModel.tasks.share(viewModel.build) },
                modifier = modifier,
                style = ButtonStyle.Text,
                enabled = busy == null,
                loading = busy == ArtifactTask.Share,
            )
            if (state.unmountable.isNotEmpty() && state.installedAs == null) {
                Button(stringResource(Res.string.run_install_instead), onInstallInstead, modifier, style = ButtonStyle.Text)
            }
        }
    }
}
