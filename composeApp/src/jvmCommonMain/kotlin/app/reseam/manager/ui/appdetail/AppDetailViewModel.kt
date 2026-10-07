package app.reseam.manager.ui.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.platform.InstalledApp
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.pick.installedTarget
import app.reseam.manager.userMessage
import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where a mount build stands on the device. */
sealed interface MountStatus {
    data object Checking : MountStatus

    data object Mounted : MountStatus

    data object NotMounted : MountStatus

    data object NotInstalled : MountStatus

    /** The store updated the app, so the boot script no longer mounts the patched version. */
    data class Updated(val installedVersion: String?) : MountStatus
}

class AppDetailViewModel(private val graph: AppGraph, private val packageName: String) : ViewModel() {
    val app: StateFlow<PatchedApp?> = graph.patchedApps.find(packageName).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val artifactActionLabel: String = graph.artifactAction.label

    val sourceAvailable: StateFlow<Boolean> = combine(app, graph.savedApks.apks) { app, _ -> app }
        .map { app -> app == null || withContext(Dispatchers.IO) { PlatformFile(app.sourceApkPath ?: app.apkPath).exists() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val installedState = MutableStateFlow<String?>(null)

    val installed: StateFlow<String?> = installedState.asStateFlow()

    private val mountState = MutableStateFlow<MountStatus>(MountStatus.Checking)

    /** Only meaningful for a mount build. */
    val mount: StateFlow<MountStatus> = mountState.asStateFlow()

    private val busyState = MutableStateFlow(false)

    /** A mount or unmount is running. */
    val busy: StateFlow<Boolean> = busyState.asStateFlow()

    init {
        viewModelScope.launch {
            val app = app.filterNotNull().first()
            if (app.installMethod == InstallMethod.MOUNT) refreshMount(app)
        }
    }

    fun mount() = changeMount { app -> app.target().let { graph.mountArtifact(packageName, PlatformFile(app.apkPath), it.apkPath, it.splitPaths) } }

    fun unmount() = changeMount { graph.unmountApp(packageName) }

    /** A mount build repatches the installed app as it is now, since a store update replaces its files. */
    fun repatch(onTarget: (PatchTarget) -> Unit) {
        val app = app.value ?: return
        if (app.installMethod == InstallMethod.INSTALL) return onTarget(app.target())
        if (busyState.value) return
        busyState.value = true
        viewModelScope.launch {
            runCatching { installedApp()?.let { graph.installedTarget(it, InstallMethod.MOUNT).copy(iconPath = app.iconPath) } }
                .onSuccess { target -> if (target == null) graph.notices.warn("${app.name} isn't installed") else onTarget(target) }
                .onFailure { graph.notices.warn(it.userMessage()) }
            busyState.value = false
        }
    }

    private fun changeMount(change: suspend (PatchedApp) -> Unit) {
        val app = app.value ?: return
        if (busyState.value) return
        busyState.value = true
        viewModelScope.launch {
            change(app)
            refreshMount(app)
            busyState.value = false
        }
    }

    private suspend fun refreshMount(app: PatchedApp) {
        val installed = installedApp()
        mountState.value = when {
            installed == null -> MountStatus.NotInstalled
            isMounted() -> MountStatus.Mounted
            installed.versionName != app.versionName -> MountStatus.Updated(installed.versionName)
            else -> MountStatus.NotMounted
        }
    }

    private suspend fun isMounted(): Boolean =
        runCatching { graph.mounter?.isMounted(packageName) == true }.onFailure { graph.notices.warn(it.userMessage()) }.getOrDefault(false)

    private suspend fun installedApp(): InstalledApp? = graph.installedApps?.query(listOf(packageName))?.firstOrNull()

    fun openArtifact() {
        val app = app.value ?: return
        viewModelScope.launch { installedState.value = graph.deliverArtifact(PlatformFile(app.apkPath)) }
    }

    fun saveArtifact() {
        val app = app.value ?: return
        viewModelScope.launch { graph.saveArtifact(PlatformFile(app.apkPath)) }
    }

    fun openInstalled() {
        installed.value?.let(graph::openApp)
    }

    fun remove(onRemoved: () -> Unit) {
        viewModelScope.launch {
            if (mountState.value == MountStatus.Mounted && !graph.unmountApp(packageName)) return@launch
            runCatching { graph.patchedApps.remove(packageName) }
                .onSuccess { onRemoved() }
                .onFailure { graph.notices.warn(it.userMessage()) }
        }
    }
}

private fun PatchedApp.target() = PatchTarget(
    name = name,
    packageName = packageName,
    versionName = versionName,
    apkPath = sourceApkPath ?: apkPath,
    splitPaths = sourceSplitPaths,
    iconPath = iconPath,
)
