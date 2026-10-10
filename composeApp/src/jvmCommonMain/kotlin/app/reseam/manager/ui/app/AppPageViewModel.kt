package app.reseam.manager.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.SavedApk
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.ui.ArtifactTasks
import app.reseam.manager.ui.nav.Route
import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MountStatus {
    data object Checking : MountStatus
    data object Mounted : MountStatus
    data object NotMounted : MountStatus
    data object NotInstalled : MountStatus

    data class Updated(val installedVersion: String?) : MountStatus
}

@OptIn(ExperimentalTime::class)
class AppPageViewModel(private val graph: AppGraph, val session: PatchSession) : ViewModel() {
    private val packageName = session.packageName

    val patched: StateFlow<PatchedApp?> = graph.patchedApps.find(packageName).stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    private val installedVersionState = MutableStateFlow<String?>(null)
    val installedVersion: StateFlow<String?> = installedVersionState.asStateFlow()

    private val outputExistsState = MutableStateFlow(false)
    val outputExists: StateFlow<Boolean> = outputExistsState.asStateFlow()

    private val patchedInstalledState = MutableStateFlow(false)
    val patchedInstalled: StateFlow<Boolean> = patchedInstalledState.asStateFlow()

    private val mountState = MutableStateFlow<MountStatus>(MountStatus.Checking)
    val mount: StateFlow<MountStatus> = mountState.asStateFlow()

    val tasks = ArtifactTasks(viewModelScope, graph.notices, graph.actions, graph.patchedApps, packageName) { refresh() }

    init {
        viewModelScope.launch { refresh() }
    }

    fun run(): Route.Run? {
        val source = session.source.value ?: return null
        val editor = session.editor.value ?: return null
        return Route.Run(packageName, source, editor.selection(), session.mount.value, Clock.System.now().toEpochMilliseconds())
    }

    fun shouldAskRunPermissions(): Boolean = !graph.settings.settings.value.askedRunPermissions

    fun runPermissionsAsked() {
        viewModelScope.launch { graph.notices.attempt { graph.settings.update { it.copy(askedRunPermissions = true) } } }
    }

    fun openPatched() {
        patched.value?.installedAs?.let(graph.actions::open)
    }

    private suspend fun refresh() {
        graph.patchedApps.find(packageName).first()?.let { app -> outputExistsState.value = withContext(Dispatchers.IO) { PlatformFile(app.apkPath).exists() } }
        val installed = graph.installedApps ?: return
        val app = installed.query(listOf(packageName)).firstOrNull()
        installedVersionState.value = app?.versionName
        val patched = graph.patchedApps.find(packageName).first() ?: return
        patchedInstalledState.value = patched.installedAs?.let { installed.query(listOf(it)).isNotEmpty() } == true
        if (patched.installMethod != InstallMethod.MOUNT) return
        mountState.value = when {
            app == null -> MountStatus.NotInstalled
            graph.actions.isMounted(packageName) -> MountStatus.Mounted
            app.versionName != patched.versionName -> MountStatus.Updated(app.versionName)
            else -> MountStatus.NotMounted
        }
    }
}
