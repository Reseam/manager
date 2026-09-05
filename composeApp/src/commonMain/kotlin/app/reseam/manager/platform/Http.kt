package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

expect suspend fun httpGetText(url: String): String

expect suspend fun httpDownload(url: String, into: PlatformFile)
