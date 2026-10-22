package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs the setup silently in update mode, which waits for Manager to exit, keeps the user's shortcut choice and starts
 * Manager again. The setup cannot replace files the running app holds, so [onStarted] closes it.
 */
class WindowsManagerInstaller(private val onStarted: () -> Unit) : ManagerInstaller {
    override fun fileName(version: String) = "reseam-manager-$version-windows-x64.exe"

    override suspend fun install(file: PlatformFile) {
        withContext(Dispatchers.IO) {
            ProcessBuilder(file.path, "/S", "/UPDATE").redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        }
        onStarted()
    }
}
