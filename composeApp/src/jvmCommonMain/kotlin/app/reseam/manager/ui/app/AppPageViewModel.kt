package app.reseam.manager.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.Notice
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.PatchedAppKey
import app.reseam.manager.data.SavedApk
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.InstalledApp
import app.reseam.manager.platform.InstalledApps
import app.reseam.manager.ui.ArtifactTasks
import app.reseam.manager.ui.nav.Route
import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.sink
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.writeString

sealed interface MountStatus {
    data object Checking : MountStatus
    data object Mounted : MountStatus
    data object NotMounted : MountStatus
    data object NotInstalled : MountStatus

    data class Updated(val installedVersion: String?) : MountStatus
}

/** What the device says about a patched build. */
data class BuildStatus(val outputExists: Boolean = false, val installed: Boolean = false, val mount: MountStatus = MountStatus.Checking)

data class BuildState(val app: PatchedApp, val status: BuildStatus)

sealed interface Presence {
    data class Installed(val versionName: String?) : Presence
    data object NotInstalled : Presence
}

@OptIn(ExperimentalTime::class)
class AppPageViewModel(private val graph: AppGraph, val session: PatchSession) : ViewModel() {
    private val packageName = session.packageName

    private val statuses = MutableStateFlow<Map<PatchedAppKey, BuildStatus>>(emptyMap())

    val builds: StateFlow<List<BuildState>> = combine(graph.patchedApps.builds(packageName), statuses) { apps, statuses ->
        apps.map { BuildState(it, statuses[it.key] ?: BuildStatus()) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val savedApks: StateFlow<List<SavedApk>> = graph.savedApks.apks
        .map { apks -> apks.filter { it.packageName == packageName }.sortedByDescending { it.savedAtEpochMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val canMount: StateFlow<Boolean> = graph.settings.settings
        .map { it.mountWithRoot && graph.mounter != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val updatingPatches: StateFlow<Boolean> = graph.bundles.updating
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val artifactKind: ArtifactAction.Kind = graph.actions.kind
    val canOpenApps: Boolean = graph.installedApps != null

    private val presenceState = MutableStateFlow<Presence?>(null)

    val presence: StateFlow<Presence?> = presenceState.asStateFlow()

    val tasks = ArtifactTasks(viewModelScope, graph.notices, graph.actions, graph.patchedApps) { refresh() }

    init {
        viewModelScope.launch { refresh() }
    }

    fun run(): Route.Run? {
        val source = session.source.value ?: return null
        val editor = session.editor.value?.takeIf { it.runnable } ?: return null
        return Route.Run(packageName, source, editor.selection(), session.mount.value, Clock.System.now().toEpochMilliseconds())
    }

    fun exportDebugReport(app: PatchedApp) {
        viewModelScope.launch {
            graph.notices.attempt {
                val file = FileKit.openFileSaver(suggestedName = "reseam-debug-${app.packageName}", defaultExtension = "txt") ?: return@attempt
                val report = graph.debugReports.of(app)
                withContext(Dispatchers.IO) { file.sink().buffered().use { it.writeString(report) } }
                graph.notices.post(Notice.Saved(file.name))
            }
        }
    }

    /** The page stays for the app's other build, and closes with its last. */
    fun remove(build: PatchedAppKey, onLast: () -> Unit) = tasks.remove(build) {
        if (graph.patchedApps.builds(packageName).first().isEmpty()) onLast()
    }

    fun shouldAskRunPermissions(): Boolean = !graph.settings.settings.value.askedRunPermissions

    fun runPermissionsAsked() {
        viewModelScope.launch { graph.notices.attempt { graph.settings.update { it.copy(askedRunPermissions = true) } } }
    }

    fun openPatched(app: PatchedApp) {
        app.installedAs?.let(graph.actions::open)
    }

    private suspend fun refresh() {
        val installed = graph.installedApps
        val original = installed?.query(listOf(packageName))?.firstOrNull()
        if (installed != null) presenceState.value = original?.let { Presence.Installed(it.versionName) } ?: Presence.NotInstalled
        statuses.value = graph.patchedApps.builds(packageName).first().associate { it.key to status(it, installed, original) }
    }

    private suspend fun status(patched: PatchedApp, installed: InstalledApps?, original: InstalledApp?): BuildStatus {
        val outputExists = withContext(Dispatchers.IO) { PlatformFile(patched.apkPath).exists() }
        if (installed == null) return BuildStatus(outputExists)
        return BuildStatus(
            outputExists = outputExists,
            installed = patched.installedAs?.let { installed.query(listOf(it)).isNotEmpty() } == true,
            mount = when {
                patched.installMethod != InstallMethod.MOUNT -> MountStatus.NotMounted
                original == null -> MountStatus.NotInstalled
                graph.actions.isMounted(packageName) -> MountStatus.Mounted
                original.versionName != patched.versionName -> MountStatus.Updated(original.versionName)
                else -> MountStatus.NotMounted
            },
        )
    }
}
