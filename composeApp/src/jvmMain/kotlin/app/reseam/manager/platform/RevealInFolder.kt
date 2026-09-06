package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File

object RevealInFolder : ArtifactAction {
    override val label = "Show in folder"

    override suspend fun run(apk: PlatformFile) = withContext(Dispatchers.IO) {
        check(apk.exists()) { "The patched APK is missing: ${apk.path}" }
        val file = File(apk.path)
        Desktop.getDesktop().open(if (apk.isDirectory()) file else file.parentFile)
    }
}
