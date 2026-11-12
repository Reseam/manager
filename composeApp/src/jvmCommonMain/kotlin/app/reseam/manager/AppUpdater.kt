package app.reseam.manager

import app.reseam.manager.data.ApkSources
import app.reseam.manager.data.AppSource
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.Failure
import app.reseam.manager.data.HeldUpdate
import app.reseam.manager.data.PatchedAppKey
import app.reseam.manager.data.PatchedAppRepository
import app.reseam.manager.data.ResolveProgress
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.BackgroundRun
import app.reseam.manager.platform.InstallFailure
import app.reseam.manager.sdk.artifactPackageName
import app.reseam.manager.sdk.path
import app.reseam.manager.sdk.reference
import app.reseam.manager.sdk.text
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.PatchRef
import app.reseam.sdk.PatchStatus
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface UpdateStatus {
    data object Waiting : UpdateStatus

    data class Preparing(val progress: ResolveProgress) : UpdateStatus

    data object Patching : UpdateStatus

    data object Installing : UpdateStatus

    /** The update stopped; the app keeps the build it had. */
    data class Failed(val error: Exception) : UpdateStatus
}

/** An update Manager started, and how far it is. Updates that replaced their app are gone from [AppUpdater.entries]. */
data class UpdateEntry(val update: AppUpdate, val status: UpdateStatus)

/**
 * Patches apps again for their [AppUpdate]s, one at a time, and replaces them on the device. An update stops before
 * replacing the app when the new build would lose patches the app has, or install beside it under another package.
 */
class AppUpdater(
    private val scope: CoroutineScope,
    private val bundles: BundleRepository,
    private val apkSources: ApkSources,
    private val patcher: Patcher,
    private val patchedApps: PatchedAppRepository,
    private val actions: PatchedAppActions,
    private val artifactAction: ArtifactAction,
    private val settings: SettingsRepository,
    private val backgroundRun: BackgroundRun?,
    private val title: suspend (appName: String) -> String,
) {
    private val entriesState = MutableStateFlow<Map<PatchedAppKey, UpdateEntry>>(emptyMap())
    val entries: StateFlow<Map<PatchedAppKey, UpdateEntry>> = entriesState.asStateFlow()

    private val lock = Mutex()

    /** Queues [updates] and runs them while the user does something else; ones already in progress stay as they are. */
    fun start(updates: List<AppUpdate>) {
        val queued = updates.filter { update -> entriesState.value[update.app.key]?.status.let { it == null || it is UpdateStatus.Failed } }
        if (queued.isEmpty()) return
        entriesState.update { entries -> entries + queued.associate { it.app.key to UpdateEntry(it, UpdateStatus.Waiting) } }
        scope.launch {
            // The update in progress, or the next one: holding waits for a first title, and the first update waits for the hold.
            val running = entries.map { entries -> entries.values.firstOrNull { it.status !is UpdateStatus.Failed } }
                .filterNotNull()
                .map { title(it.update.app.name) }
                .distinctUntilChanged()
            backgroundRun?.hold(running) { run(queued) } ?: run(queued)
        }
    }

    /** Runs [updates] in order. What each came to is in [entries]: gone when it replaced its app, failed otherwise. */
    suspend fun run(updates: List<AppUpdate>) = each(updates, ::update)

    /** Downloads the app versions [updates] bring, so updating later only patches and installs; the device keeps its apps. */
    suspend fun download(updates: List<AppUpdate>) = each(updates.filter { it.source is AppSource.Download }) { update ->
        apkSources.resolve(update.app.packageName, update.source) { set(update, UpdateStatus.Preparing(it)) }
    }

    /** Runs [step] for each update in turn: one that finishes is gone from [entries], one that fails stays with why. */
    private suspend fun each(updates: List<AppUpdate>, step: suspend (AppUpdate) -> Unit) {
        for (update in updates) {
            lock.withLock {
                try {
                    step(update)
                    entriesState.update { it - update.app.key }
                } catch (cancelled: CancellationException) {
                    entriesState.update { it - update.app.key }
                    throw cancelled
                } catch (error: Exception) {
                    set(update, UpdateStatus.Failed(error))
                }
            }
        }
    }

    /** The library takes the new build only once it is on the device, so it always describes what the device runs. */
    private suspend fun update(update: AppUpdate) {
        val staged = patch(update)
        val installedAs = try {
            rejection(update, staged)?.let { rejected ->
                // The same run makes the same build, so it isn't offered again until patching again would change it.
                patchedApps.hold(update.app.key, HeldUpdate(update.source.versionName, staged.outcome.record))
                throw rejected
            }
            // Where nothing installs apps (desktop), the update is the new build.
            if (artifactAction.kind == ArtifactAction.Kind.Install) install(update, staged) else update.app.installedAs
        } catch (error: Throwable) {
            patcher.discard(staged.job)
            throw error
        }
        patcher.keep(staged, installedAs)
    }

    /** Runs the app's patches on the version [update] brings; the app keeps its build until [Patcher.keep]. */
    private suspend fun patch(update: AppUpdate): StagedBuild {
        val app = update.app
        set(update, UpdateStatus.Preparing(ResolveProgress.Preparing))
        val apk = apkSources.resolve(app.packageName, update.source) { set(update, UpdateStatus.Preparing(it)) }
        val versionName = apk.saved?.versionName ?: update.source.versionName
        val patches = bundles.active.filterNotNull().first().values.flatten()
        val editor = PatchEditor.of(app, patches, versionName, settings.settings.value.allowIncompatiblePatches)
        if (!editor.runnable) throw Failure.UpdateUnsupported(app.name)
        set(update, UpdateStatus.Patching)
        return patcher.stage(PatchJob(app.packageName, app.name, app.iconPath, update.source, apk, editor.selection(), app.installMethod), onEvent = {})
    }

    /**
     * Why [staged] must not replace the app, or null when it may: it would lose a patch the app has (one that fails, or
     * is skipped because one it needs fails), or install beside it under another package.
     */
    private suspend fun rejection(update: AppUpdate, staged: StagedBuild): Failure? {
        val app = update.app
        val missing = staged.outcome.results.filter { it.status !is PatchStatus.Applied }.map { it.patch }.toSet()
        val lost = app.patches.filter { PatchRef(it.bundle, it.id) in missing }
        // A store update already replaced the build the app had, so there is nothing left to keep.
        if (lost.isNotEmpty() && update.reason !is UpdateReason.Original) {
            val catalog = bundles.patches.filterNotNull().first().values.flatten().associateBy { it.reference }
            return Failure.UpdateLosesPatches(app.name, lost.map { catalog[PatchRef(it.bundle, it.id)]?.text()?.name ?: it.name })
        }
        val installedAs = app.installedAs?.takeIf { app.installMethod == InstallMethod.INSTALL } ?: return null
        val packageName = artifactPackageName(PlatformFile(staged.outcome.output.path))
        return if (packageName != installedAs) Failure.UpdateRenames(app.name, packageName) else null
    }

    /** Installs or mounts [staged] over the app, returning the package it runs as. */
    private suspend fun install(update: AppUpdate, staged: StagedBuild): String {
        val app = update.app
        val output = staged.outcome.output.path
        set(update, UpdateStatus.Installing)
        return when (app.installMethod) {
            InstallMethod.INSTALL -> when (val delivery = actions.deliver(app.key, PlatformFile(output))) {
                is Delivery.Installed -> delivery.packageName
                is Delivery.Conflict -> throw Failure.InstallFailed(if (delivery.newer) InstallFailure.Downgrade else InstallFailure.Conflict, null)
                Delivery.Other -> throw Failure.UpdateNotInstalled(app.name)
            }
            InstallMethod.MOUNT -> {
                val apk = staged.job.apk
                if (!actions.mount(app.copy(apkPath = output, sourceApkPath = apk.apkPath, sourceSplitPaths = apk.splitPaths))) throw Failure.MountFailed(app.name)
                app.packageName
            }
        }
    }

    private fun set(update: AppUpdate, status: UpdateStatus) {
        entriesState.update { it + (update.app.key to UpdateEntry(update, status)) }
    }
}
