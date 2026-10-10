package app.reseam.manager

import app.reseam.manager.data.AnnouncementRepository
import app.reseam.manager.data.ApkSources
import app.reseam.manager.data.AppIdentityReader
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
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.SigningKeyRepository
import app.reseam.manager.platform.ApkPresentationReader
import app.reseam.manager.platform.AppMounter
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.BackgroundRun
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.InstalledApps
import app.reseam.manager.platform.SourceSession
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appIdentities = AppIdentityReader(cacheDirectory / "icons", presentation)
    val notices = Notices()
    val settings = SettingsRepository(JsonStore(dataDirectory, "settings.json", Settings.serializer(), Settings()))
    val bundles = BundleRepository(JsonStore(dataDirectory, "bundles.json", BundleLibrary.serializer(), BundleLibrary()), dataDirectory / "bundles")
    val bundleSyncer = BundleSyncer(scope, bundles, settings, notices)
    val patchedApps = PatchedAppRepository(JsonStore(dataDirectory, "patched.json", PatchedAppLibrary.serializer(), PatchedAppLibrary()), dataDirectory / "patched")
    val signingKeys = SigningKeyRepository(dataDirectory / "signing")
    val savedApks = SavedApkRepository(JsonStore(dataDirectory, "saved-apks.json", SavedApkLibrary.serializer(), SavedApkLibrary()), dataDirectory / "saved-apks", appIdentities)
    val apkSources = ApkSources(appIdentities, Downloader(DefaultDownloaderBaseUrl, sourceSession, DefaultSource), savedApks, patchedApps, installedApps, mounter, device, cacheDirectory / "originals")
    val actions = PatchedAppActions(artifactAction, mounter, installedApps, patchedApps, settings, notices)
    val patcher = Patcher(bundles, signingKeys, patchedApps, savedApks, settings, actions)
    val announcements = AnnouncementRepository(settings)
    val managerUpdates = ManagerUpdater(scope, settings, cacheDirectory / "manager-update", device, artifactAction)

    init {
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
    }
}
