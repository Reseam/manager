package app.reseam.manager

import app.reseam.manager.data.AppSource
import app.reseam.manager.data.AppliedPatch
import app.reseam.manager.data.BundleRef
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.PatchedAppKey
import app.reseam.manager.data.PatchedAppRepository
import app.reseam.manager.data.ResolvedApk
import app.reseam.manager.data.SavedApkRepository
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.SigningKeyRepository
import app.reseam.manager.data.ref
import app.reseam.manager.sdk.ReseamSdk
import app.reseam.manager.sdk.chosen
import app.reseam.manager.sdk.path
import app.reseam.manager.sdk.reference
import app.reseam.manager.sdk.text
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class PatchJob(
    val packageName: String,
    val name: String,
    val iconPath: String?,
    val source: AppSource,
    val apk: ResolvedApk,
    val selection: PatchSelection,
    val installMethod: InstallMethod,
) {
    val key: PatchedAppKey get() = PatchedAppKey(packageName, installMethod)
}

data class PatchedBuild(val output: String, val outcome: PatchOutcome)

/** A run's output, still apart from the library: the app's previous build stays until [Patcher.keep]. */
data class StagedBuild(val job: PatchJob, val outcome: PatchOutcome, val bundles: List<BundleRef>)

/** Runs the engine and keeps its output: the library entry and the source download. */
class Patcher(
    private val bundles: BundleRepository,
    private val signingKeys: SigningKeyRepository,
    private val patchedApps: PatchedAppRepository,
    private val savedApks: SavedApkRepository,
    private val settings: SettingsRepository,
) {
    suspend fun patch(job: PatchJob, onEvent: (RunEvent) -> Unit): PatchedBuild = keep(stage(job, onEvent))

    suspend fun stage(job: PatchJob, onEvent: (RunEvent) -> Unit): StagedBuild = try {
        val sources = bundles.usable()
        val outcome = ReseamSdk.patch(
            PatchRequest(
                apkPath = job.apk.apkPath,
                splitPaths = job.apk.splitPaths,
                bundlePaths = sources.map { it.path },
                trust = bundles.trust(),
                selection = job.selection,
                output = PatchOutput.Auto(patchedApps.stage(job.key).absolutePath()),
                signing = signingKeys.identity(),
                installMethod = job.installMethod,
                leaveOutFailed = true,
            ),
            onEvent = onEvent,
        )
        StagedBuild(job, outcome, sources.map { it.ref })
    } catch (error: Throwable) {
        discard(job)
        throw error
    } finally {
        signingKeys.refresh()
    }

    /** Takes [staged] into the library in place of the app's previous build; [installedAs] when it is already on the device. */
    @OptIn(ExperimentalTime::class)
    suspend fun keep(staged: StagedBuild, installedAs: String? = null): PatchedBuild = try {
        val (job, outcome) = staged
        val output = patchedApps.publish(job.key, PlatformFile(outcome.output.path))
        val sourceInUse = patchedApps.apps.first().any { it.key != job.key && it.sourceApkPath == job.apk.apkPath }
        val dropSource = job.source is AppSource.Download && job.installMethod == InstallMethod.INSTALL && !settings.settings.value.keepDownloads && !sourceInUse
        val names = bundles.active.filterNotNull().first().values.flatten().associate { it.reference to it.spec.name }
        val applied = outcome.results.filter { it.status is PatchStatus.Applied }
        val versionName = job.apk.saved?.versionName ?: job.source.versionName
        patchedApps.save(
            PatchedApp(
                packageName = job.packageName,
                name = job.name,
                versionName = versionName,
                apkPath = output.absolutePath(),
                sourceApkPath = job.apk.apkPath.takeUnless { dropSource },
                iconPath = job.iconPath ?: job.apk.saved?.iconPath,
                sourceSplitPaths = job.apk.splitPaths,
                patches = applied.filter { it.chosen }
                    .map { AppliedPatch(it.patch.id, it.patch.bundle, names[it.patch] ?: it.patch.text) },
                patchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                installMethod = job.installMethod,
                installedAs = installedAs,
                source = job.source,
                record = outcome.record,
                bundles = staged.bundles,
            ),
        )
        if (dropSource) job.apk.saved?.let { savedApks.remove(it.id) }
        PatchedBuild(output.absolutePath(), outcome)
    } catch (error: Throwable) {
        discard(staged.job)
        throw error
    }

    suspend fun discard(job: PatchJob) = withContext(NonCancellable) { patchedApps.discardStaged(job.key) }
}
