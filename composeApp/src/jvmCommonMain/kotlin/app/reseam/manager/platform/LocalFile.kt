package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

/** Null where the native engine can't open a picked file in place and it has to be copied. */
expect val PlatformFile.enginePath: String?
