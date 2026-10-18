package app.reseam.manager.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Icons
import org.jetbrains.compose.resources.stringResource

@Composable
/** [anyway] when the build is without some of the chosen patches. */
fun ArtifactAction.Kind.deliverLabel(busy: Boolean = false, anyway: Boolean = false): String = stringResource(
    when (this) {
        ArtifactAction.Kind.Install -> when {
            busy -> Res.string.artifact_installing
            anyway -> Res.string.artifact_install_anyway
            else -> Res.string.artifact_install
        }
        ArtifactAction.Kind.Reveal -> Res.string.artifact_reveal
    },
)

val ArtifactAction.Kind.deliverIcon: ImageVector
    get() = when (this) {
        ArtifactAction.Kind.Install -> Icons.Download
        ArtifactAction.Kind.Reveal -> Icons.FolderOpen
    }

@Composable
fun ArtifactAction.Kind.shareLabel(): String = stringResource(
    when (this) {
        ArtifactAction.Kind.Install -> Res.string.artifact_share
        ArtifactAction.Kind.Reveal -> Res.string.artifact_save
    },
)
