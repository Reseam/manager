package app.reseam.manager

import app.reseam.manager.data.AppSource
import app.reseam.manager.data.AppliedPatch
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.PatchedAppRepository
import app.reseam.manager.data.ResolvedApk
import app.reseam.manager.data.SavedApkRepository
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.SigningKeyRepository
import app.reseam.manager.sdk.ReseamSdk
import app.reseam.manager.sdk.chosen
import app.reseam.manager.sdk.path
import app.reseam.manager.sdk.reference
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.PatchOutcome
import app.reseam.sdk.PatchOutput
import app.reseam.sdk.PatchRequest
import app.reseam.sdk.PatchSelection
import app.reseam.sdk.PatchStatus
import app.reseam.sdk.RunEvent
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

data class PatchJob(
    val packageName: String,
    val name: String,
    val iconPath: String?,
    val source: AppSource,
    val apk: ResolvedApk,
    val selection: PatchSelection,
    val installMethod: InstallMethod,
)

data class PatchedBuild(val output: String, val outcome: PatchOutcome)

/** Runs the engine and keeps its output: the library entry, the replaced mount and the source download. */
class Patcher(
    private val bundles: BundleRepository,
    private val signingKeys: SigningKeyRepository,
    private val patchedApps: PatchedAppRepository,
    private val savedApks: SavedApkRepository,
    private val settings: SettingsRepository,
    private val actions: PatchedAppActions,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun patch(job: PatchJob, onEvent: (RunEvent) -> Unit): PatchedBuild = try {
        val outcome = ReseamSdk.patch(
            PatchRequest(
                apkPath = job.apk.apkPath,
                splitPaths = job.apk.splitPaths,
                bundlePaths = bundles.paths(),
                trust = bundles.trust(),
                selection = job.selection,
                output = PatchOutput.Auto(patchedApps.stage(job.packageName).absolutePath()),
                signing = signingKeys.files(),
                installMethod = job.installMethod,
            ),
            onEvent = onEvent,
        )
        val output = patchedApps.publish(job.packageName, PlatformFile(outcome.output.path))
        if (job.installMethod == InstallMethod.INSTALL) actions.unmountReplaced(job.packageName)
        val dropSource = job.source is AppSource.Download && job.installMethod == InstallMethod.INSTALL && !settings.settings.value.keepDownloads
        val names = bundles.patches.value.orEmpty().values.flatten().associate { it.reference to it.spec.name }
        patchedApps.save(
            PatchedApp(
                packageName = job.packageName,
                name = job.name,
                versionName = job.apk.saved?.versionName ?: job.source.versionName,
                apkPath = output.absolutePath(),
                sourceApkPath = job.apk.apkPath.takeUnless { dropSource },
                iconPath = job.iconPath ?: job.apk.saved?.iconPath,
                sourceSplitPaths = job.apk.splitPaths,
                patches = outcome.results.filter { it.status is PatchStatus.Applied && it.chosen }
                    .map { AppliedPatch(it.patch.substringAfter('/'), it.patch.substringBefore('/'), names[it.patch] ?: it.patch) },
                patchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                installMethod = job.installMethod,
            ),
        )
        if (dropSource) job.apk.saved?.let { savedApks.remove(it.id) }
        PatchedBuild(output.absolutePath(), outcome)
    } catch (error: Throwable) {
        withContext(NonCancellable) { patchedApps.discardStaged(job.packageName) }
        throw error
    } finally {
        signingKeys.refresh()
    }
}
