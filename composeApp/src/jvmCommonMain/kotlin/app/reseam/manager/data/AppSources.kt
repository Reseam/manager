package app.reseam.manager.data

import app.reseam.manager.platform.AppMounter
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.InstalledApps
import app.reseam.manager.platform.enginePath
import app.reseam.sdk.CompatiblePackage
import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppSource {
    val versionName: String?

    @Serializable
    data class Download(override val versionName: String?) : AppSource

    @Serializable
    data class Installed(override val versionName: String?) : AppSource

    @Serializable
    data class Saved(val id: String, override val versionName: String?) : AppSource

    @Serializable
    data class File(val path: String, override val versionName: String?) : AppSource
}

data class ResolvedApk(val apkPath: String, val splitPaths: List<String>, val saved: SavedApk?)

sealed interface ResolveProgress {
    data object Preparing : ResolveProgress
    data class Downloading(val written: Long, val total: Long?) : ResolveProgress
}

data class VersionOption(val versionName: String?, val patchCount: Int)

private val NewestFirst = Comparator<String> { a, b -> if (isNewerVersion(a, b)) -1 else if (isNewerVersion(b, a)) 1 else 0 }

/** Patches for any app take every version, so they decide whether the newest is offered but are not counted. */
fun versionOptions(entries: List<CompatiblePackage>, universal: Boolean): List<VersionOption> {
    val anyVersion = entries.count { it.versions.isEmpty() }
    val pinned = entries.flatMap { it.versions }.groupingBy { it }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy(NewestFirst) { it.key })
        .map { (version, count) -> VersionOption(version, count + anyVersion) }
    return pinned + listOfNotNull(VersionOption(null, anyVersion).takeIf { anyVersion > 0 || universal || pinned.isEmpty() })
}

data class PickedApp(val packageName: String, val source: AppSource)

class ApkSources(
    private val identities: AppIdentityReader,
    private val downloader: Downloader,
    private val savedApks: SavedApkRepository,
    private val patchedApps: PatchedAppRepository,
    private val installedApps: InstalledApps?,
    private val mounter: AppMounter?,
    private val device: DeviceProfile?,
    private val originals: PlatformFile,
) {
    suspend fun resolve(packageName: String, source: AppSource, onProgress: (ResolveProgress) -> Unit): ResolvedApk = when (source) {
        is AppSource.Download -> download(packageName, source.versionName, onProgress)
        is AppSource.Installed -> installed(packageName)
        is AppSource.Saved -> savedApks.find(source.id)?.let { ResolvedApk(it.path, emptyList(), it) } ?: throw Failure.SourceMissing()
        is AppSource.File -> if (withContext(Dispatchers.IO) { PlatformFile(source.path).exists() }) ResolvedApk(source.path, emptyList(), null) else throw Failure.SourceMissing()
    }

    suspend fun pick(file: PlatformFile): PickedApp {
        val path = file.enginePath
        if (path != null) {
            val identity = identities.read(path)
            return PickedApp(identity.packageName ?: throw Failure.NoPackageName(), AppSource.File(path, identity.versionName))
        }
        val saved = savedApks.save(file.name.substringAfterLast('.', "apk"), SavedApkOrigin.File) { withContext(Dispatchers.IO) { file.copyTo(it) } }
        return PickedApp(saved.packageName ?: throw Failure.NoPackageName(), AppSource.Saved(saved.id, saved.versionName))
    }

    private suspend fun download(packageName: String, versionName: String?, onProgress: (ResolveProgress) -> Unit): ResolvedApk {
        onProgress(ResolveProgress.Preparing)
        val build = downloader.builds(packageName, versionName).builds.best(device) ?: throw Failure.NoFittingBuild()
        val sourceBuild = SourceBuild(downloader.source, build.id)
        val apk = savedApks.downloaded(sourceBuild) ?: run {
            val target = downloader.resolve(build)
            val extension = when (build.container) {
                BuildContainer.Apk -> "apk"
                BuildContainer.Bundle -> "apkm"
            }
            savedApks.save(extension, SavedApkOrigin.Download, sourceBuild) { file ->
                downloader.download(target, file) { written, total -> onProgress(ResolveProgress.Downloading(written, total)) }
            }
        }
        if (apk.packageName != packageName) {
            savedApks.remove(apk.id)
            throw Failure.WrongDownload(packageName)
        }
        return ResolvedApk(apk.path, emptyList(), apk)
    }

    /** A mounted app's paths show the patched APKs, so the originals are copied out from under the mount. */
    private suspend fun installed(packageName: String): ResolvedApk {
        val app = installedApps?.query(listOf(packageName))?.firstOrNull() ?: throw Failure.NotInstalled(packageName)
        val mounter = mounter
        if (mounter == null || patchedApps.find(packageName).first()?.installMethod != InstallMethod.MOUNT || !mounter.isMounted(packageName)) {
            return ResolvedApk(app.apkPath, app.splitPaths, null)
        }
        val copies = mounter.copyOriginals(packageName, originals / packageName)
        return ResolvedApk(copies.first().absolutePath(), copies.drop(1).map { it.absolutePath() }, null)
    }
}
