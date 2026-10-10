package app.reseam.manager

import app.reseam.manager.data.Failure
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.PatchedAppRepository
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.exportArtifact
import app.reseam.manager.platform.ApkSet
import app.reseam.manager.platform.AppMounter
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.ArtifactOutcome
import app.reseam.manager.platform.InstallFailure
import app.reseam.manager.platform.InstalledApps
import app.reseam.manager.sdk.apkSet
import app.reseam.manager.sdk.openApkArchive
import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** What installing a patched app came to, for the screen that asked. */
sealed interface Delivery {
    data class Installed(val packageName: String) : Delivery

    /** An app with the same package is signed differently and has to go first. */
    data class Conflict(val packageName: String) : Delivery

    data object Other : Delivery
}

/** Installs, mounts, shares and opens patched apps, posting the outcome as a notice. */
class PatchedAppActions(
    private val artifactAction: ArtifactAction,
    private val mounter: AppMounter?,
    private val installedApps: InstalledApps?,
    private val patchedApps: PatchedAppRepository,
    private val settings: SettingsRepository,
    private val notices: Notices,
) {
    val kind: ArtifactAction.Kind get() = artifactAction.kind

    suspend fun deliver(packageName: String, artifact: PlatformFile): Delivery {
        val outcome = notices.attempt { artifactAction.run(artifact, settings.settings.value.useSystemInstaller) } ?: return Delivery.Other
        when (outcome) {
            is ArtifactOutcome.Installed -> {
                patchedApps.markInstalled(packageName, outcome.packageName)
                return Delivery.Installed(outcome.packageName)
            }
            is ArtifactOutcome.Failed -> {
                if (outcome.reason == InstallFailure.Conflict && outcome.packageName != null) return Delivery.Conflict(outcome.packageName)
                notices.error(Failure.InstallFailed(outcome.reason, outcome.detail))
            }
            ArtifactOutcome.Cancelled -> notices.post(Notice.InstallCancelled)
            ArtifactOutcome.OpenedInstaller -> notices.post(Notice.InstallUnconfirmed)
            ArtifactOutcome.TimedOut -> notices.post(Notice.InstallTimedOut)
            ArtifactOutcome.Revealed, ArtifactOutcome.PermissionRequested -> Unit
        }
        return Delivery.Other
    }

    /** Uninstalls the differently signed [conflicting] app, then installs the patched one in its place. */
    suspend fun replace(packageName: String, artifact: PlatformFile, conflicting: String): Delivery =
        if (notices.attempt { artifactAction.uninstall(conflicting) } == true) deliver(packageName, artifact) else Delivery.Other

    suspend fun mount(app: PatchedApp): Boolean =
        notices.attempt {
            val mounter = checkNotNull(mounter)
            val artifact = PlatformFile(app.apkPath)
            val patched = withContext(Dispatchers.IO) {
                if (artifact.isDirectory()) artifact.list().map { it.absolutePath() }.filter { it.endsWith(".apk") } else listOf(artifact.absolutePath())
            }
            withApkSet(patched.first(), patched.drop(1)) { apks ->
                if (!mounter.matches(app.packageName, apks)) withApkSet(app.sourceApkPath ?: app.apkPath, app.sourceSplitPaths) { mounter.install(app.packageName, it) }
                mounter.mount(app.packageName, apks)
            }
            patchedApps.markInstalled(app.packageName, app.packageName)
            notices.post(Notice.AppMounted)
        } != null

    suspend fun unmount(packageName: String): Boolean =
        notices.attempt {
            checkNotNull(mounter).unmount(packageName)
            notices.post(Notice.AppUnmounted)
        } != null

    /** The library keeps one build per app, so a mount it is about to forget comes off the device first. */
    suspend fun unmountReplaced(packageName: String) {
        if (isMountedBuild(packageName)) unmount(packageName)
    }

    /** Forgets the patched app and deletes its build, unmounting it first. */
    suspend fun remove(packageName: String): Boolean {
        if (isMountedBuild(packageName) && !unmount(packageName)) return false
        return notices.attempt { patchedApps.remove(packageName) } != null
    }

    private suspend fun isMountedBuild(packageName: String): Boolean =
        patchedApps.find(packageName).first()?.installMethod == InstallMethod.MOUNT && isMounted(packageName)

    suspend fun isMounted(packageName: String): Boolean = notices.attempt { mounter?.isMounted(packageName) } == true

    suspend fun share(artifact: PlatformFile) {
        if (notices.attempt { artifactAction.share(artifact) } == false) save(artifact)
    }

    fun open(packageName: String) {
        runCatching { checkNotNull(installedApps).launch(packageName) }.onFailure(notices::error)
    }

    private suspend fun save(artifact: PlatformFile) {
        notices.attempt {
            val destination = FileKit.openFileSaver(
                suggestedName = artifact.name.removeSuffix(".apk"),
                defaultExtension = if (artifact.isDirectory()) "apks" else "apk",
            ) ?: return@attempt
            exportArtifact(artifact, destination)
            notices.post(Notice.AppSaved(destination.name))
        }
    }

    private suspend fun <T> withApkSet(apkPath: String, splitPaths: List<String>, block: suspend (ApkSet) -> T): T =
        withContext(Dispatchers.IO) { openApkArchive(apkPath, splitPaths) }.use { block(withContext(Dispatchers.IO) { it.apkSet() }) }
}
