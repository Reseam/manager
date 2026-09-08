package app.reseam.manager.platform

import app.reseam.manager.sdk.ApkArchive

/** The name and icon a launcher shows for an app. The icon is encoded, ready to keep as a file. */
class AppPresentation(val label: String?, val icon: ByteArray?)

/** Reads an archive's presentation. Platforms with a resource loader localize the label and render every icon kind. */
fun interface ApkPresentationReader {
    fun read(archive: ApkArchive): AppPresentation
}
