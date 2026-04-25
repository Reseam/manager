package app.reseam.manager.data.platform

import app.reseam.manager.domain.installer.PatchedAppInstaller
import app.reseam.manager.domain.manager.OutputPathProvider
import app.reseam.manager.patcher.PatchArtifact
import app.reseam.manager.ui.model.PatchInput
import java.awt.Desktop
import java.io.File

class DesktopOutputPathProvider(
    private val outputDirectory: File = desktopDataFile("output").also { it.mkdirs() },
) : OutputPathProvider {
    override fun outputFor(input: PatchInput): String {
        val safeName = input.displayName
            .lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .ifBlank { "patched" }
        return File(outputDirectory, "$safeName.reseamed.apk").absolutePath
    }
}

class DesktopPatchedAppInstaller : PatchedAppInstaller {
    override suspend fun install(artifact: PatchArtifact) {
        val file = File(artifact.path)
        require(file.exists()) { "Patched artifact does not exist: ${artifact.path}" }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file.parentFile ?: file)
        }
    }
}
