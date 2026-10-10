package app.reseam.manager.platform

import app.reseam.manager.sdk.ApkArchive

class AppPresentation(val label: String?, val icon: ByteArray?)

fun interface ApkPresentationReader {
    fun read(archive: ApkArchive): AppPresentation
}
