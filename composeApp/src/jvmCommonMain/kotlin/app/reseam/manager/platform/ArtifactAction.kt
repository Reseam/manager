package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

/** What the platform does with a finished patched APK: install it, or reveal it. */
interface ArtifactAction {
    val label: String
    suspend fun run(apk: PlatformFile)
}
