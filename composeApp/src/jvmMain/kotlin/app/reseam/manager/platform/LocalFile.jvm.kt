package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

actual suspend fun PlatformFile.localCopy(cacheDirectory: PlatformFile): PlatformFile = this
