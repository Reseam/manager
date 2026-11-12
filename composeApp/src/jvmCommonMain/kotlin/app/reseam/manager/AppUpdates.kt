package app.reseam.manager

import app.reseam.manager.data.AndroidFit
import app.reseam.manager.data.AndroidRequirements
import app.reseam.manager.data.AppSource
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.Catalog
import app.reseam.manager.data.Downloader
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.PatchedAppRepository
import app.reseam.manager.data.SavedApk
import app.reseam.manager.data.SavedApkRepository
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.best
import app.reseam.manager.data.isNewerVersion
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.InstalledApp
import app.reseam.manager.platform.InstalledApps
import app.reseam.sdk.BuildRecord
import app.reseam.sdk.ChangesRequest
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.changes
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** Why a patched app should be patched again. */
sealed interface UpdateReason {
    /** Patches it took up changed, new ones apply to it, or ones it has are gone. */
    data object Patches : UpdateReason

    /** The patches support a newer [versionName] of the app, which Manager downloads. */
    data class Version(val versionName: String) : UpdateReason

    /** The store updated the installed app it was patched from, to [versionName]. */
    data class Original(val versionName: String) : UpdateReason
}

/**
 * Patching [app] again from [source], with the patches and options it has. A [held] update stopped before on the same
 * version and patches, so it isn't offered again; it is listed only while that stop shows.
 */
data class AppUpdate(val app: PatchedApp, val source: AppSource, val reason: UpdateReason, val held: Boolean = false)

/**
 * Which patched apps an update would change. Only apps that are on the device count, since an update replaces
 * them; without a device (desktop) every build counts.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdatePlanner(
    private val bundles: BundleRepository,
    private val patchedApps: PatchedAppRepository,
    private val savedApks: SavedApkRepository,
    private val settings: SettingsRepository,
    private val installedApps: InstalledApps?,
    private val downloader: Downloader,
    private val requirements: AndroidRequirements,
    private val device: DeviceProfile?,
) {
    /** The newest build the download service has of apps whose patches take any version. */
    private val newest = MutableStateFlow<Map<String, String>>(emptyMap())

    private val rechecks = MutableStateFlow(0)

    /** Every update found, held ones included. */
    val found: Flow<List<AppUpdate>> = combine(patchedApps.apps, bundles.active.filterNotNull(), newest, requirements.known, rechecks) { apps, patches, newest, known, _ ->
        PlanInputs(apps, patches, newest, known)
    }.mapLatest { (apps, patches, newest, known) -> plan(apps, patches, newest, known) }.flowOn(Dispatchers.Default)

    /** The updates to offer. */
    val updates: Flow<List<AppUpdate>> = found.map { found -> found.filterNot { it.held } }

    /** Reads the device's apps again: the store may have updated one. */
    fun recheck() {
        rechecks.update { it + 1 }
    }

    /** One app the service can't answer for keeps what it had; the others still update. */
    suspend fun checkNewest() {
        val catalog = bundles.catalog.filterNotNull().first()
        val apps = patchedApps.apps.first().filterNot { it.followsOriginal }
        for (app in apps) {
            val newer = catalog.versions(app.packageName).mapNotNull { it.versionName }.filter { version -> app.versionName?.let { isNewerVersion(version, it) } ?: true }
            requirements.check(app.packageName, newer)
        }
        val unpinned = apps
            .filter { catalog.versions(it.packageName).first().versionName == null }
            .map { it.packageName }
            .distinct()
        for (packageName in unpinned) {
            val version = try {
                downloader.builds(packageName, null).builds.best(device)?.versionName
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                continue
            }
            if (version != null) newest.update { it + (packageName to version) }
        }
    }

    private suspend fun plan(apps: List<PatchedApp>, patches: Map<String, List<PatchMetadata>>, newest: Map<String, String>, known: Map<String, Int>): List<AppUpdate> {
        val catalog = Catalog.of(patches)
        val metadata = patches.values.flatten()
        val saved = savedApks.apks.first()
        val installed = installedApps?.query(apps.flatMap { listOfNotNull(it.packageName, it.installedAs) }.distinct())?.associateBy { it.packageName }
        return apps.filter { installed == null || it.installedAs in installed }
            .mapNotNull { app -> plan(app, installed?.get(app.packageName), catalog, metadata, saved, newest, known) }
    }

    private suspend fun plan(
        app: PatchedApp,
        original: InstalledApp?,
        catalog: Catalog,
        metadata: List<PatchMetadata>,
        saved: List<SavedApk>,
        newest: Map<String, String>,
        known: Map<String, Int>,
    ): AppUpdate? {
        val versionName = app.versionName ?: return null
        if (app.followsOriginal) {
            val installed = original?.versionName ?: return null
            if (installed != versionName) return offer(AppUpdate(app, AppSource.Installed(installed), UpdateReason.Original(installed)), editor(app, installed, metadata), metadata)
            return patchUpdate(app, versionName, metadata, saved)
        }
        val option = requirements.fit(app.packageName, catalog.versions(app.packageName), known).first()
        val target = (if (option.android is AndroidFit.Needs) null else option.versionName ?: newest[app.packageName])?.takeIf { isNewerVersion(it, versionName) }
        val newer = target?.let { offer(AppUpdate(app, AppSource.Download(it), UpdateReason.Version(it)), editor(app, it, metadata), metadata) }
        // A newer version the patches fail on still leaves patch changes for the version the app has.
        if (newer != null && !newer.held) return newer
        return patchUpdate(app, versionName, metadata, saved) ?: newer
    }

    private suspend fun patchUpdate(app: PatchedApp, versionName: String, metadata: List<PatchMetadata>, saved: List<SavedApk>): AppUpdate? {
        val editor = editor(app, versionName, metadata)
        if (!patchesChanged(app, editor, metadata)) return null
        return offer(AppUpdate(app, currentSource(app, saved), UpdateReason.Patches), editor, metadata)
    }

    /**
     * [update] as it is offered: none when its patches can't run on the version it brings, and held when an update to
     * that version was refused before and patching again would make the same build.
     */
    private fun offer(update: AppUpdate, editor: PatchEditor, metadata: List<PatchMetadata>): AppUpdate? {
        if (!editor.runnable) return null
        val held = update.app.held?.takeIf { it.versionName == update.source.versionName } ?: return update
        return update.copy(held = !rebuilds(held.record, editor, metadata))
    }

    /**
     * Builds patched before Manager kept their record compare the versions of the sources their patches came from; the
     * others ask the SDK whether patching again, with the choice the app would be patched with, makes another build.
     */
    private fun patchesChanged(app: PatchedApp, editor: PatchEditor, metadata: List<PatchMetadata>): Boolean {
        val record = app.record ?: run {
            val installed = bundles.installed()
            return app.bundles.any { source ->
                app.patches.any { it.bundle == source.name } && installed.firstOrNull { it.id == source.id }?.version != source.version
            }
        }
        // Patches the SDK can't plan can't patch the app again, so there is no update to offer; its page says why.
        if (editor.failure != null) return false
        return rebuilds(record, editor, metadata)
    }

    /** Whether patching again with [editor]'s choice makes a build other than [record]. */
    private fun rebuilds(record: BuildRecord, editor: PatchEditor, metadata: List<PatchMetadata>): Boolean =
        changes(ChangesRequest(record.copy(selection = editor.selection()), metadata)).isNotEmpty()

    private fun editor(app: PatchedApp, versionName: String?, metadata: List<PatchMetadata>): PatchEditor =
        PatchEditor.of(app, metadata, versionName, settings.settings.value.allowIncompatiblePatches)

    /** The copy the app was patched from, when Manager still has it; otherwise the same version, downloaded. */
    private suspend fun currentSource(app: PatchedApp, saved: List<SavedApk>): AppSource {
        val source = app.source
        if (source is AppSource.Installed && app.followsOriginal) return source
        saved.firstOrNull { it.path == app.sourceApkPath }?.let { return AppSource.Saved(it.id, it.versionName) }
        if (source is AppSource.File && withContext(Dispatchers.IO) { PlatformFile(source.path).exists() }) return source
        return AppSource.Download(app.versionName)
    }
}

/**
 * The original stays on the device beside the build, so updates follow it as the store updates it: a mount, or an
 * install from the installed app under another package. Other builds follow the versions the patches support.
 */
private val PatchedApp.followsOriginal: Boolean
    get() = installMethod == InstallMethod.MOUNT || (source is AppSource.Installed && installedAs != packageName)

private data class PlanInputs(val apps: List<PatchedApp>, val patches: Map<String, List<PatchMetadata>>, val newest: Map<String, String>, val known: Map<String, Int>)
