package app.reseam.manager.ui

import app.reseam.manager.Delivery
import app.reseam.manager.Notices
import app.reseam.manager.PatchedAppActions
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.PatchedAppRepository
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

enum class ArtifactTask { Deliver, Mount, Unmount, Share, Remove }

/** Acts on an app's patched build, one task at a time; [onSettled] gets the package a task installed, if any. */
class ArtifactTasks(
    scope: CoroutineScope,
    notices: Notices,
    private val actions: PatchedAppActions,
    private val patchedApps: PatchedAppRepository,
    private val packageName: String,
    private val onSettled: suspend (installedAs: String?) -> Unit,
) {
    private val tasks = SingleTask<ArtifactTask>(scope, notices)
    val busy: StateFlow<ArtifactTask?> = tasks.running

    private val conflictState = MutableStateFlow<String?>(null)

    /** The differently signed app that blocks the install, until the user decides. */
    val conflict: StateFlow<String?> = conflictState.asStateFlow()

    fun deliver() = launch(ArtifactTask.Deliver) { app -> delivered(actions.deliver(packageName, PlatformFile(app.apkPath))) }

    fun replace() {
        val conflicting = conflictState.value ?: return
        conflictState.value = null
        launch(ArtifactTask.Deliver) { app -> delivered(actions.replace(packageName, PlatformFile(app.apkPath), conflicting)) }
    }

    fun keepInstalled() {
        conflictState.value = null
    }

    fun share() = launch(ArtifactTask.Share) { app ->
        actions.share(PlatformFile(app.apkPath))
        null
    }

    fun mount() = launch(ArtifactTask.Mount) { app -> packageName.takeIf { actions.mount(app) } }

    fun unmount() = launch(ArtifactTask.Unmount) {
        actions.unmount(packageName)
        null
    }

    fun remove(onRemoved: () -> Unit) = launch(ArtifactTask.Remove) {
        if (actions.remove(packageName)) onRemoved()
        null
    }

    private fun delivered(delivery: Delivery): String? {
        conflictState.value = (delivery as? Delivery.Conflict)?.packageName
        return (delivery as? Delivery.Installed)?.packageName
    }

    private fun launch(task: ArtifactTask, block: suspend (PatchedApp) -> String?) = tasks.launch(task) {
        val app = patchedApps.find(packageName).first() ?: return@launch
        var installed: String? = null
        try {
            installed = block(app)
        } finally {
            onSettled(installed)
        }
    }
}
