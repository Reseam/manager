package app.reseam.manager

import androidx.compose.runtime.staticCompositionLocalOf
import app.reseam.manager.data.AppIdentityReader
import app.reseam.manager.data.BundleLibrary
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.OfficialReleaseInfo
import app.reseam.manager.data.fetchManagerRelease
import app.reseam.manager.data.isNewerVersion
import app.reseam.manager.data.JsonStore
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    /** A newer Manager release published by the configured API, if any. */
    val managerUpdate: StateFlow<OfficialReleaseInfo?> get() = managerUpdateState
    private val managerUpdateState = MutableStateFlow<OfficialReleaseInfo?>(null)

    /** Silent on failure: an unreachable or empty index is not something the user can act on. */
    fun checkManagerUpdate() {
        scope.launch {
            managerUpdateState.value = runCatching { fetchManagerRelease(settings.settings.value.apiBaseUrl) }
                .getOrNull()
                ?.takeIf { isNewerVersion(it.version, ManagerVersion) }
        }
    }

    fun syncOfficialBundle(force: Boolean = false): Job =
        scope.launch {
            bundles.patches.filterNotNull().first()
            val settings = settings.settings.value
            if (!force && !settings.checkUpdatesDaily && bundles.installed().any { it.official }) return@launch
            runCatching { bundles.syncOfficial(settings.apiBaseUrl, force) }
                .onSuccess { staged -> if (staged != null) notices.post("Official patches: confirm the signer under Settings, Bundles") }
                .onFailure { notices.post("Official patches: ${it.userMessage()}") }
        }
}

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph is not provided") }
