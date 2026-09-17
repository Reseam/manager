package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath

actual val PlatformFile.enginePath: String? get() = absolutePath()
