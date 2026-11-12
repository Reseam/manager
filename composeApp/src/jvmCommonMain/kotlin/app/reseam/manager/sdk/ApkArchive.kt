package app.reseam.manager.sdk

import app.reseam.manager.platform.ApkSet
import app.reseam.sdk.ApkInspection
import app.reseam.sdk.ApkMetadata
import app.reseam.sdk.ApplicationIcon
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ApkArchive : AutoCloseable {
    val metadata: ApkMetadata
    val basePath: String
    val splitPaths: List<String>

    fun icon(): ApplicationIcon?
}

internal fun openApkArchive(path: String, splitPaths: List<String> = emptyList()): ApkArchive = NativeApkArchive(ApkInspection(path, splitPaths))

/** A single APK, or every APK of a split set. */
internal suspend fun artifactApks(artifact: PlatformFile): List<String> = withContext(Dispatchers.IO) {
    if (artifact.isDirectory()) artifact.list().map { it.absolutePath() }.filter { it.endsWith(".apk") } else listOf(artifact.absolutePath())
}

/** The package [artifact] installs as. */
internal suspend fun artifactPackageName(artifact: PlatformFile): String {
    val apks = artifactApks(artifact)
    val packageName = withContext(Dispatchers.IO) { openApkArchive(apks.first(), apks.drop(1)).use { it.metadata.identity.packageName } }
    return checkNotNull(packageName) { "The patched build names no package" }
}

internal fun ApkArchive.apkSet() = ApkSet(
    versionCode = checkNotNull(metadata.identity.versionCode) { "The APK's manifest has no version code" },
    base = basePath,
    splits = metadata.splitNames.zip(splitPaths).toMap(),
)

private class NativeApkArchive(private val inspection: ApkInspection) : ApkArchive {
    override val metadata: ApkMetadata by lazy { inspection.metadata() }
    override val basePath: String get() = inspection.basePath()
    override val splitPaths: List<String> get() = inspection.splitPaths()

    override fun icon(): ApplicationIcon? = inspection.applicationIcon()

    override fun close() = inspection.close()
}
