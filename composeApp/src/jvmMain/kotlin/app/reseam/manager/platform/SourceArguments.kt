package app.reseam.manager.platform

import app.reseam.manager.data.SourceInput
import io.github.vinceglb.filekit.PlatformFile
import java.awt.Desktop
import java.io.File
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** The .reseam file among launch arguments, which file managers pass when the user opens one with Reseam Manager. */
fun List<String>.sourceInput(): SourceInput? =
    firstNotNullOfOrNull { argument -> File(argument).takeIf { it.isFile && it.extension == "reseam" }?.let { SourceInput.File(PlatformFile(it)) } }

/** macOS delivers opened files as events rather than launch arguments. */
fun openedFiles(): Flow<List<String>> {
    val opened = MutableSharedFlow<List<String>>(replay = 1, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
        Desktop.getDesktop().setOpenFileHandler { event -> opened.tryEmit(event.files.map { it.path }) }
    }
    return opened
}
