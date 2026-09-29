package app.reseam.manager.platform

import app.reseam.manager.Notices
import io.github.vinceglb.filekit.PlatformFile

/** What the platform does with a finished patched APK: install it, or reveal it. */
interface ArtifactAction {
    val label: String
    suspend fun run(apk: PlatformFile): ArtifactOutcome
}

sealed interface ArtifactOutcome {
    data object Revealed : ArtifactOutcome

    data object PermissionRequested : ArtifactOutcome

    /** [packageName] is the installed app's, which a patch may have renamed from the source's. */
    data class Installed(val packageName: String) : ArtifactOutcome

    data object Cancelled : ArtifactOutcome

    data class Failed(val message: String) : ArtifactOutcome
}

fun Notices.report(outcome: ArtifactOutcome) {
    when (outcome) {
        is ArtifactOutcome.Installed -> info("Patched app installed")
        ArtifactOutcome.Cancelled -> info("Install cancelled")
        is ArtifactOutcome.Failed -> warn("Install failed: ${outcome.message}")
        ArtifactOutcome.Revealed, ArtifactOutcome.PermissionRequested -> Unit
    }
}
