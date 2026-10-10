package app.reseam.manager.platform

import app.reseam.manager.data.Failure
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.path
import java.awt.Desktop
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RevealInFolder : ArtifactAction {
    override val kind = ArtifactAction.Kind.Reveal

    override suspend fun run(artifact: PlatformFile, useSystemInstaller: Boolean): ArtifactOutcome = withContext(Dispatchers.IO) {
        if (!artifact.exists()) throw Failure.OutputMissing()
        val file = File(artifact.path)
        Desktop.getDesktop().open(if (artifact.isDirectory()) file else file.parentFile)
        ArtifactOutcome.Revealed
    }

    override suspend fun share(artifact: PlatformFile): Boolean = false

    override suspend fun uninstall(packageName: String): Boolean = false
}
