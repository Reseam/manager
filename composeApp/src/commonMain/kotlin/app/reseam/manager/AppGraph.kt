package app.reseam.manager

import androidx.compose.runtime.staticCompositionLocalOf
import app.reseam.manager.data.BundleLibrary
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.OfficialReleaseInfo
import app.reseam.manager.data.fetchManagerRelease
import app.reseam.manager.data.isNewerVersion
import app.reseam.manager.data.JsonStore
import app.reseam.manager.data.PatchedAppLibrary
import app.reseam.manager.data.PatchedAppRepository
import app.reseam.manager.data.Settings
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.InstalledApps
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppGraph(
    dataDirectory: PlatformFile,
    val cacheDirectory: PlatformFile,
    val installedApps: InstalledApps?,
    val artifactAction: ArtifactAction,
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val notices = Notices()
    val settings = SettingsRepository(JsonStore(dataDirectory, "settings.json", Settings.serializer(), Settings()))
    val bundles = BundleRepository(JsonStore(dataDirectory, "bundles.json", BundleLibrary.serializer(), BundleLibrary()), dataDirectory / "bundles")
    val patchedApps = PatchedAppRepository(JsonStore(dataDirectory, "patched.json", PatchedAppLibrary.serializer(), PatchedAppLibrary()), dataDirectory / "patched")

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

    fun syncOfficialBundle(force: Boolean = false) {
        scope.launch {
            val settings = settings.settings.value
            if (!force && !settings.checkUpdatesDaily && bundles.installed().any { it.official }) return@launch
            runCatching { bundles.syncOfficial(settings.apiBaseUrl, force) }
                .onSuccess { staged -> if (staged != null) notices.post("Official patches: confirm the signer under Settings, Bundles") }
                .onFailure { notices.post("Official patches: ${it.userMessage()}") }
        }
    }
}

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph is not provided") }
