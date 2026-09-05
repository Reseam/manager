package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

/** A copy of a picked file the native engine can open by path. Platforms with real file paths return the file itself. */
expect suspend fun PlatformFile.localCopy(cacheDirectory: PlatformFile): PlatformFile
