package app.reseam.manager

import app.reseam.manager.data.AndroidRequirementLibrary
import app.reseam.manager.data.AndroidRequirements
import app.reseam.manager.data.AnnouncementRepository
import app.reseam.manager.data.ApkSources
import app.reseam.manager.data.AppIdentityReader
import app.reseam.manager.data.AppListingLibrary
import app.reseam.manager.data.AppListings
import app.reseam.manager.data.BundleLibrary
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.BundleSyncer
import app.reseam.manager.data.DefaultDownloaderBaseUrl
import app.reseam.manager.data.DefaultSource
import app.reseam.manager.data.Downloader
import app.reseam.manager.data.JsonStore
import app.reseam.manager.data.ManagerUpdater
import app.reseam.manager.data.PatchedAppLibrary
import app.reseam.manager.data.PatchedAppRepository
import app.reseam.manager.data.SavedApkLibrary
import app.reseam.manager.data.SavedApkRepository
import app.reseam.manager.data.Settings
import app.reseam.manager.data.SettingsSerializer
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.SigningKeyRepository
import app.reseam.manager.data.SourceImports
import app.reseam.manager.platform.ApkPresentationReader
import app.reseam.manager.platform.AppMounter
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.cacheTextResponses
import app.reseam.manager.platform.BackgroundRun
import app.reseam.manager.platform.BackgroundUpdates
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.InstalledApps
import app.reseam.manager.platform.ManagerInstaller
import app.reseam.manager.platform.SourceSession
import app.reseam.manager.resources.Res
import app.reseam.manager.resources.notify_updating_title
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class AppGraph(
    dataDirectory: PlatformFile,
    cacheDirectory: PlatformFile,
    val installedApps: InstalledApps?,
    artifactAction: ArtifactAction,
    val mounter: AppMounter?,
    presentation: ApkPresentationReader,
    sourceSession: SourceSession,
    device: DeviceProfile?,
    val backgroundRun: BackgroundRun?,
    val backgroundUpdates: BackgroundUpdates?,
    managerInstaller: ManagerInstaller?,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appIdentities = AppIdentityReader(cacheDirectory / "icons", presentation)
    val notices = Notices()
    val settings = SettingsRepository(JsonStore(dataDirectory, "settings.json", SettingsSerializer, Settings()))
    val bundles = BundleRepository(JsonStore(dataDirectory, "bundles.json", BundleLibrary.serializer(), BundleLibrary()), dataDirectory / "bundles")
    val bundleSyncer = BundleSyncer(scope, bundles, settings, notices)
    val sourceImports = SourceImports(scope, bundles)
    val patchedApps = PatchedAppRepository(JsonStore(dataDirectory, "patched.json", PatchedAppLibrary.serializer(), PatchedAppLibrary()), dataDirectory / "patched")
    val signingKeys = SigningKeyRepository(dataDirectory / "signing")
    val savedApks = SavedApkRepository(JsonStore(dataDirectory, "saved-apks.json", SavedApkLibrary.serializer(), SavedApkLibrary()), dataDirectory / "saved-apks", appIdentities)
    private val downloader = Downloader(DefaultDownloaderBaseUrl, sourceSession, DefaultSource)
    val androidRequirements = AndroidRequirements(JsonStore(cacheDirectory, "android-requirements.json", AndroidRequirementLibrary.serializer(), AndroidRequirementLibrary()), downloader, device)
    val apkSources = ApkSources(appIdentities, downloader, androidRequirements, savedApks, patchedApps, installedApps, mounter, device, cacheDirectory / "originals")
    val appListings = AppListings(JsonStore(cacheDirectory, "app-listings.json", AppListingLibrary.serializer(), AppListingLibrary()), cacheDirectory / "listing-icons", downloader)
    val actions = PatchedAppActions(artifactAction, mounter, installedApps, patchedApps, settings, notices)
    val patcher = Patcher(bundles, signingKeys, patchedApps, savedApks, settings)
    val announcements = AnnouncementRepository(settings)
    val managerUpdates = ManagerUpdater(scope, settings, cacheDirectory / "manager-update", managerInstaller)
    val appUpdates = AppUpdatePlanner(bundles, patchedApps, savedApks, settings, installedApps, downloader, androidRequirements, device)
    val appUpdater = AppUpdater(scope, bundles, apkSources, patcher, patchedApps, actions, artifactAction, settings, backgroundRun) { getString(Res.string.notify_updating_title, it) }
    val updateChecks = UpdateChecks(scope, announcements, managerUpdates, bundleSyncer, appUpdates)
    val debugReports = DebugReports(installedApps, actions, bundles, signingKeys, settings, device)

    /** Starts every update found that isn't already running. */
    fun updateAllApps() {
        scope.launch { appUpdater.start(appUpdates.updates.first()) }
    }

    init {
        cacheTextResponses(cacheDirectory / "http")
        scope.launch {
            notices.attempt { bundles.load() }
                ?.filterNot { (bundle) -> bundle.official }
                ?.forEach { (bundle, problem) -> notices.post(Notice.BundleRemoved(bundle.name, problem)) }
        }
        scope.launch {
            notices.attempt {
                savedApks.clearStaging()
                patchedApps.clearStaging()
            }
        }
        backgroundUpdates?.schedule()
        scope.launch {
            bundles.catalog.filterNotNull().collectLatest { catalog ->
                val packageNames = catalog.apps.map { it.packageName }
                val installed = installedApps?.query(packageNames)?.map { it.packageName }.orEmpty().toSet()
                // Names are a nicety: without one the package name shows, and the next launch asks again.
                try {
                    appListings.refresh(packageNames - installed)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                }
            }
        }
    }
}
