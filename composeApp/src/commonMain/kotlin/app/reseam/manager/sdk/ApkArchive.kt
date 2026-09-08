package app.reseam.manager.sdk

/** A launcher icon as the APK declares it. Adaptive layers sit on a 108dp canvas of which the central 72dp is shown. */
sealed interface ApkIcon {
    class Bitmap(val bytes: ByteArray) : ApkIcon
    class Adaptive(val background: IconLayer, val foreground: IconLayer) : ApkIcon
}

sealed interface IconLayer {
    class Bitmap(val bytes: ByteArray) : IconLayer
    class Color(val argb: Int) : IconLayer
}

/** An opened APK, APKM, or XAPK. Component paths stay valid until the archive is closed. */
interface ApkArchive : AutoCloseable {
    val metadata: ApkMetadata
    val basePath: String
    val splitPaths: List<String>

    /** Null when the icon is a vector drawable, which only a platform renderer can draw. */
    fun icon(): ApkIcon?
}

internal expect fun openApkArchive(path: String): ApkArchive
