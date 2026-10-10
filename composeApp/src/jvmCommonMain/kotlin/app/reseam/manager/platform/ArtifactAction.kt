package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

interface ArtifactAction {
    val kind: Kind

    suspend fun run(artifact: PlatformFile, useSystemInstaller: Boolean): ArtifactOutcome

    /** Hands the APK to the system share sheet; false where there is none. */
    suspend fun share(artifact: PlatformFile): Boolean

    /** Returns whether [packageName] is gone; the system asks the user first. */
    suspend fun uninstall(packageName: String): Boolean

    enum class Kind { Install, Reveal }
}

sealed interface ArtifactOutcome {
    data object Revealed : ArtifactOutcome

    data object OpenedInstaller : ArtifactOutcome

    data object PermissionRequested : ArtifactOutcome

    data class Installed(val packageName: String) : ArtifactOutcome

    data object Cancelled : ArtifactOutcome

    data object TimedOut : ArtifactOutcome

    /** [packageName] is the app the patched build installs as. */
    data class Failed(val reason: InstallFailure, val detail: String?, val packageName: String?) : ArtifactOutcome
}

enum class InstallFailure { Conflict, Storage, Incompatible, Invalid, Blocked, Other }
