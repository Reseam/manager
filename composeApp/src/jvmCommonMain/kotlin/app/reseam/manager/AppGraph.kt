package app.reseam.manager

import androidx.compose.runtime.staticCompositionLocalOf
import app.reseam.manager.data.AppIdentityReader
import app.reseam.manager.data.BundleLibrary
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.JsonStore
import app.reseam.manager.data.ManagerUpdater
import app.reseam.manager.data.PatchedAppLibrary
import app.reseam.manager.data.PatchedAppRepository
import app.reseam.manager.data.DefaultDownloaderBaseUrl
import app.reseam.manager.data.DefaultSource
import app.reseam.manager.data.Downloader
import app.reseam.manager.data.SavedApkLibrary
import app.reseam.manager.data.SavedApkRepository
import app.reseam.manager.data.Settings
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.SigningKeyRepository
import app.reseam.manager.data.SyncScope
import app.reseam.manager.platform.ApkPresentationReader
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.InstalledApps
import app.reseam.manager.platform.SourceSession
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AppGraph(
    dataDirectory: PlatformFile,
    val cacheDirectory: PlatformFile,
    val installedApps: InstalledApps?,
    val artifactAction: ArtifactAction,
    presentation: ApkPresentationReader,
    sourceSession: SourceSession,
    /** Null on desktop, which patches for a device it can't see. */
    val device: DeviceProfile?,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val notices = Notices()
    val appIdentities = AppIdentityReader(cacheDirectory / "icons", presentation)
    val settings = SettingsRepository(JsonStore(dataDirectory, "settings.json", Settings.serializer(), Settings()))
    val bundles = BundleRepository(JsonStore(dataDirectory, "bundles.json", BundleLibrary.serializer(), BundleLibrary()), dataDirectory / "bundles")
    val patchedApps = PatchedAppRepository(JsonStore(dataDirectory, "patched.json", PatchedAppLibrary.serializer(), PatchedAppLibrary()), dataDirectory / "patched")
    val signingKeys = SigningKeyRepository(dataDirectory / "signing")
    val savedApks = SavedApkRepository(JsonStore(dataDirectory, "saved-apks.json", SavedApkLibrary.serializer(), SavedApkLibrary()), dataDirectory / "saved-apks", appIdentities)
    val downloader = Downloader(DefaultDownloaderBaseUrl, sourceSession, DefaultSource)
    val managerUpdates = ManagerUpdater(scope, settings, cacheDirectory / "manager-update", device, artifactAction)

    init {
        scope.launch {
            runCatching { bundles.load() }
                .onSuccess { removed -> removed.filterNot { (bundle) -> bundle.official }.forEach { (bundle, problem) -> notices.post("${bundle.name} was removed. ${problem.userMessage(bundle.name).orEmpty()}".trim()) } }
                .onFailure { notices.post("Bundles: ${it.userMessage()}") }
        }
        scope.launch {
            runCatching { savedApks.clearStaging() }.onFailure { notices.post("Saved APKs: ${it.userMessage()}") }
        }
    }

    fun syncBundles(force: Boolean = false): Job =
        scope.launch {
            bundles.patches.filterNotNull().first()
            val settings = settings.settings.value
            val checks = when {
                force -> SyncScope.All
                settings.autoUpdateBundles -> SyncScope.Due
                else -> SyncScope.None
            }
            runCatching { bundles.sync(settings.apiBaseUrl, checks) }
                .onSuccess { (prompt, failures) ->
                    failures.singleOrNull()?.let { notices.post("${it.bundle}: ${it.error.userMessage()}") }
                    if (failures.size > 1) notices.post("${failures.size} bundles couldn't update: ${failures.joinToString { it.bundle }}")
                    prompt?.let { notices.post("${it.metadata.name}: confirm the signer under Settings, Bundles") }
                }
                .onFailure { notices.post("Bundles: ${it.userMessage()}") }
        }
}

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph is not provided") }
