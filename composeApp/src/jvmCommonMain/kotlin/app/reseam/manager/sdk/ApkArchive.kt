package app.reseam.manager.sdk

import app.reseam.manager.platform.ApkSet
import app.reseam.sdk.ApkInspection
import app.reseam.sdk.ApkMetadata
import app.reseam.sdk.ApplicationIcon

interface ApkArchive : AutoCloseable {
    val metadata: ApkMetadata
    val basePath: String
    val splitPaths: List<String>

    fun icon(): ApplicationIcon?
}

internal fun openApkArchive(path: String, splitPaths: List<String> = emptyList()): ApkArchive = NativeApkArchive(ApkInspection(path, splitPaths))

internal fun ApkArchive.apkSet() = ApkSet(metadata.versionCode?.toLong() ?: 0, basePath, metadata.splitNames.zip(splitPaths).toMap())

private class NativeApkArchive(private val inspection: ApkInspection) : ApkArchive {
    override val metadata: ApkMetadata by lazy { inspection.metadata() }
    override val basePath: String get() = inspection.basePath()
    override val splitPaths: List<String> get() = inspection.splitPaths()

    override fun icon(): ApplicationIcon? = inspection.applicationIcon()

    override fun close() = inspection.close()
}
