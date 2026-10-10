package app.reseam.manager.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

class DesktopDirectories {
    private val local = if (DesktopOperatingSystem.current == DesktopOperatingSystem.Windows) {
        val path = checkNotNull(System.getenv("LOCALAPPDATA")?.takeIf(String::isNotBlank)) { "LOCALAPPDATA is not set" }
        PlatformFile(File(path, "app.reseam.manager"))
    } else null

    val data: PlatformFile = local?.div("data") ?: FileKit.filesDir
    val cache: PlatformFile = local?.div("cache") ?: FileKit.cacheDir

    fun prepareTemporaryDirectory() {
        if (local == null) return
        val temporary = (cache / "tmp").file
        check(!temporary.exists() || temporary.deleteRecursively()) { "Could not clear temporary files in $temporary" }
        check(temporary.mkdirs()) { "Could not create temporary directory $temporary" }
    }

    fun openLog() {
        val root = local ?: return
        val logs = root / "logs"
        logs.createDirectories()
        val log = File(logs.file, "manager.log")
        val previous = File(logs.file, "manager.previous.log")
        if (log.exists()) java.nio.file.Files.move(log.toPath(), previous.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        val stream = PrintStream(FileOutputStream(log), true, Charsets.UTF_8)
        System.setOut(stream)
        System.setErr(stream)
    }
}
