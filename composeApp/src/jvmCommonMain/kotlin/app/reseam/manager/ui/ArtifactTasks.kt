package app.reseam.manager.ui

import app.reseam.manager.Delivery
import app.reseam.manager.Notices
import app.reseam.manager.PatchedAppActions
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.PatchedAppKey
import app.reseam.manager.data.PatchedAppRepository
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

enum class ArtifactTask { Deliver, Mount, Unmount, Share, Remove }

data class ArtifactJob(val task: ArtifactTask, val build: PatchedAppKey)

/** An install of [build] that waits on the user, because [conflict] is in the way. */
data class PendingReplace(val build: PatchedAppKey, val conflict: Delivery.Conflict)

/** Acts on patched builds, one task at a time; [onSettled] gets the package a task installed, if any. */
class ArtifactTasks(
    scope: CoroutineScope,
    notices: Notices,
    private val actions: PatchedAppActions,
    private val patchedApps: PatchedAppRepository,
    private val onSettled: suspend (installedAs: String?) -> Unit,
) {
    private val tasks = SingleTask<ArtifactJob>(scope, notices)
    val busy: StateFlow<ArtifactJob?> = tasks.running

    private val pendingState = MutableStateFlow<PendingReplace?>(null)

    /** The installed app that blocks an install, until the user decides. */
    val pending: StateFlow<PendingReplace?> = pendingState.asStateFlow()

    fun deliver(build: PatchedAppKey) = launch(ArtifactTask.Deliver, build) { app -> delivered(build, actions.deliver(build, PlatformFile(app.apkPath))) }

    fun replace() {
        val (build, conflict) = pendingState.value ?: return
        pendingState.value = null
        launch(ArtifactTask.Deliver, build) { app -> delivered(build, actions.replace(build, PlatformFile(app.apkPath), conflict.packageName)) }
    }

    fun keepInstalled() {
        pendingState.value = null
    }

    fun share(build: PatchedAppKey) = launch(ArtifactTask.Share, build) { app ->
        actions.share(PlatformFile(app.apkPath))
        null
    }

    fun mount(build: PatchedAppKey) = launch(ArtifactTask.Mount, build) { app -> app.packageName.takeIf { actions.mount(app) } }

    fun unmount(build: PatchedAppKey) = launch(ArtifactTask.Unmount, build) { app ->
        actions.unmount(app.packageName)
        null
    }

    fun remove(build: PatchedAppKey, onRemoved: suspend () -> Unit) = launch(ArtifactTask.Remove, build) {
        if (actions.remove(build)) onRemoved()
        null
    }

    private fun delivered(build: PatchedAppKey, delivery: Delivery): String? {
        pendingState.value = (delivery as? Delivery.Conflict)?.let { PendingReplace(build, it) }
        return (delivery as? Delivery.Installed)?.packageName
    }

    private fun launch(task: ArtifactTask, build: PatchedAppKey, block: suspend (PatchedApp) -> String?) = tasks.launch(ArtifactJob(task, build)) {
        val app = patchedApps.find(build).first() ?: return@launch
        var installed: String? = null
        try {
            installed = block(app)
        } finally {
            onSettled(installed)
        }
    }
}
