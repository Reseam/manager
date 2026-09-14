package app.reseam.manager.sdk

import app.reseam.sdk.ApkInspection
import app.reseam.sdk.ApkMetadata
import app.reseam.sdk.ApplicationIcon

/** An opened APK, APKM, or XAPK. Component paths stay valid until the archive is closed. */
interface ApkArchive : AutoCloseable {
    val metadata: ApkMetadata
    val basePath: String
    val splitPaths: List<String>

    /** Null when the icon is a vector drawable, which only a platform renderer can draw. */
    fun icon(): ApplicationIcon?
}

internal fun openApkArchive(path: String): ApkArchive = NativeApkArchive(ApkInspection(path, emptyList()))

private class NativeApkArchive(private val inspection: ApkInspection) : ApkArchive {
    override val metadata: ApkMetadata by lazy { inspection.metadata() }
    override val basePath: String get() = inspection.basePath()
    override val splitPaths: List<String> get() = inspection.splitPaths()

    override fun icon(): ApplicationIcon? = inspection.applicationIcon()

    override fun close() = inspection.close()
}
