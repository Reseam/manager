package app.reseam.manager.data.platform

import app.reseam.manager.domain.sources.FilePicker
import app.reseam.manager.domain.sources.PickedFile
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopFilePicker : FilePicker {
    override suspend fun pickApk(): PickedFile? =
        chooseFile(
            title = "Choose APK",
            filter = FileNameExtensionFilter("Android packages", "apk", "apks", "xapk"),
        )

    override suspend fun pickPatchBundle(): PickedFile? =
        chooseFile(
            title = "Choose patch bundle",
            filter = FileNameExtensionFilter("Reseam patch bundles", "reseam", "json", "zip"),
        )

    private suspend fun chooseFile(title: String, filter: FileNameExtensionFilter): PickedFile? =
        withContext(Dispatchers.IO) {
            val chooser = JFileChooser().apply {
                dialogTitle = title
                fileSelectionMode = JFileChooser.FILES_ONLY
                fileFilter = filter
            }
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return@withContext null
            chooser.selectedFile.toPickedFile()
        }

    private fun File.toPickedFile(): PickedFile =
        PickedFile(displayName = name, path = absolutePath)
}
