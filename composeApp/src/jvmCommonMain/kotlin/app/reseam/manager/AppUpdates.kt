package app.reseam.manager

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

/** Patching [app] again from [source], with the patches and options it has. */
data class AppUpdate(val app: PatchedApp, val source: AppSource, val reason: UpdateReason)

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
    private val device: DeviceProfile?,
) {
    /** The newest build the download service has of apps whose patches take any version. */
    private val newest = MutableStateFlow<Map<String, String>>(emptyMap())

    private val rechecks = MutableStateFlow(0)

    val updates: Flow<List<AppUpdate>> = combine(patchedApps.apps, bundles.active.filterNotNull(), newest, rechecks) { apps, patches, newest, _ ->
        Triple(apps, patches, newest)
    }.mapLatest { (apps, patches, newest) -> plan(apps, patches, newest) }

    /** Reads the device's apps again: the store may have updated one. */
    fun recheck() {
        rechecks.update { it + 1 }
    }

    /** One app the service can't answer for keeps what it had; the others still update. */
    suspend fun checkNewest() {
        val catalog = bundles.catalog.filterNotNull().first()
        val unpinned = patchedApps.apps.first()
            .filter { it.source is AppSource.Download && catalog.versions(it.packageName).first().versionName == null }
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

    private suspend fun plan(apps: List<PatchedApp>, patches: Map<String, List<PatchMetadata>>, newest: Map<String, String>): List<AppUpdate> {
        val catalog = Catalog.of(patches)
        val metadata = patches.values.flatten()
        val saved = savedApks.apks.first()
        val installed = installedApps?.query(apps.flatMap { listOfNotNull(it.packageName, it.installedAs) }.distinct())?.associateBy { it.packageName }
        return apps.filter { installed == null || it.installedAs in installed }
            .mapNotNull { app -> plan(app, installed?.get(app.packageName), catalog, metadata, saved, newest) }
    }

    private suspend fun plan(
        app: PatchedApp,
        original: InstalledApp?,
        catalog: Catalog,
        metadata: List<PatchMetadata>,
        saved: List<SavedApk>,
        newest: Map<String, String>,
    ): AppUpdate? {
        val versionName = app.versionName ?: return null
        if (app.installMethod == InstallMethod.MOUNT || app.source is AppSource.Installed) {
            val installed = original?.versionName ?: return null
            if (installed != versionName) return AppUpdate(app, AppSource.Installed(installed), UpdateReason.Original(installed))
        } else {
            val target = catalog.versions(app.packageName).first().versionName ?: newest[app.packageName]
            if (target != null && isNewerVersion(target, versionName)) return AppUpdate(app, AppSource.Download(target), UpdateReason.Version(target))
        }
        if (!patchesChanged(app, versionName, metadata)) return null
        return AppUpdate(app, currentSource(app, saved), UpdateReason.Patches)
    }

    /**
     * Builds patched before Manager kept their record compare the versions of the sources their patches came from; the
     * others ask the SDK whether patching again, with the choice the app would be patched with, makes another build.
     */
    private fun patchesChanged(app: PatchedApp, versionName: String, metadata: List<PatchMetadata>): Boolean {
        val record = app.record ?: run {
            val installed = bundles.installed()
            return app.bundles.any { source ->
                app.patches.any { it.bundle == source.name } && installed.firstOrNull { it.id == source.id }?.version != source.version
            }
        }
        val editor = PatchEditor.of(app, metadata, versionName, settings.settings.value.allowIncompatiblePatches)
        // Patches the SDK can't plan can't patch the app again, so there is no update to offer; its page says why.
        if (editor.failure != null) return false
        return changes(ChangesRequest(record.copy(selection = editor.selection()), metadata)).isNotEmpty()
    }

    /** The copy the app was patched from, when Manager still has it; otherwise the same version, downloaded. */
    private suspend fun currentSource(app: PatchedApp, saved: List<SavedApk>): AppSource {
        val source = app.source
        if (source is AppSource.Installed) return source
        saved.firstOrNull { it.path == app.sourceApkPath }?.let { return AppSource.Saved(it.id, it.versionName) }
        if (source is AppSource.File && withContext(Dispatchers.IO) { PlatformFile(source.path).exists() }) return source
        return AppSource.Download(app.versionName)
    }
}
