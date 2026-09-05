package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Content URIs from the document picker are not readable by native code; copy them into app-private cache. */
actual suspend fun PlatformFile.localCopy(cacheDirectory: PlatformFile): PlatformFile = withContext(Dispatchers.IO) {
    cacheDirectory.createDirectories()
    val target = cacheDirectory / name.replace(Regex("[^A-Za-z0-9._-]+"), "-")
    copyTo(target)
    target
}
