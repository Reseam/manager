package app.reseam.manager.data.platform

import app.reseam.manager.domain.installer.PatchedAppInstaller
import app.reseam.manager.patcher.PatchArtifact
import java.awt.Desktop
import java.io.File

class DesktopPatchedAppInstaller : PatchedAppInstaller {
    override suspend fun install(artifact: PatchArtifact) {
        val file = File(artifact.path)
        check(file.exists()) { "Patched artifact does not exist: ${artifact.path}" }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file.parentFile ?: file)
        }
    }
}
