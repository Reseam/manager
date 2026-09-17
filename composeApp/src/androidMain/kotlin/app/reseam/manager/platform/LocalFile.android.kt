package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

/** Content URIs from the document picker are not readable by native code. */
actual val PlatformFile.enginePath: String? get() = null
