package app.reseam.manager.ui

import app.reseam.manager.AppGraph
import app.reseam.manager.AppUpdate
import app.reseam.manager.UpdateStatus
import app.reseam.manager.data.Failure
import app.reseam.manager.resources.Res
import app.reseam.manager.resources.check_verify
import app.reseam.manager.resources.try_again
import app.reseam.manager.resources.update
import app.reseam.manager.ui.components.AppLook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.jetbrains.compose.resources.StringResource

/** An app [update] could patch again; [status] is set once Manager started it. */
data class ShownUpdate(val update: AppUpdate, val status: UpdateStatus?) {
    val look: AppLook get() = AppLook(update.app.name, update.app.packageName, update.app.iconPath)

    /** Waiting for the user: not started, or stopped. */
    val startable: Boolean get() = status == null || status is UpdateStatus.Failed

    /** The download site's check the update stopped at, which only the user can pass. */
    val humanCheck: String? get() = ((status as? UpdateStatus.Failed)?.error as? Failure.HumanCheckRequired)?.url
}

/** What starting [update] does: update, pass the site's check first, or try again after a stop. */
fun updateAction(update: ShownUpdate): StringResource = when {
    update.humanCheck != null -> Res.string.check_verify
    update.status == null -> Res.string.update
    else -> Res.string.try_again
}

/** Updates in progress come first, then the ones found; a stop shows only while the app still needs that update. */
fun AppGraph.shownUpdates(): Flow<List<ShownUpdate>> = combine(appUpdates.updates, appUpdater.entries) { found, started ->
    val running = started.values.filter { it.status !is UpdateStatus.Failed }
    val runningKeys = running.map { it.update.app.key }.toSet()
    running.map { ShownUpdate(it.update, it.status) } + found.filter { it.app.key !in runningKeys }.map { ShownUpdate(it, started[it.app.key]?.status) }
}
