package app.reseam.manager.platform

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.reseam.manager.AppGraph
import app.reseam.manager.AppUpdate
import app.reseam.manager.ReseamApplication
import app.reseam.manager.UpdateStatus
import app.reseam.manager.data.AppSource
import app.reseam.manager.data.Failure
import app.reseam.manager.resources.Res
import app.reseam.manager.resources.notify_manager_update_body
import app.reseam.manager.resources.notify_update_check_body
import app.reseam.manager.resources.notify_update_failed_body
import app.reseam.manager.resources.notify_update_failed_title
import app.reseam.manager.resources.notify_updated_title
import app.reseam.manager.resources.notify_updates_available_title
import app.reseam.manager.resources.notify_updates_ready_title
import app.reseam.manager.resources.update_available
import app.reseam.sdk.InstallMethod
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * Checks for a new Manager, new patches and new app versions, as opening Manager does. With auto-update on and
 * notifications allowed, so no app changes without the user hearing, patched apps update; when installing would ask
 * first (Play Protect), they get ready and wait for one tap. Otherwise the user hears they can update. Each finding is
 * told once.
 */
class AppUpdateWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val notifications = UpdateNotifications(context)

    override suspend fun doWork(): Result {
        val graph = (applicationContext as ReseamApplication).graph
        graph.updateChecks.refresh()
        tellManagerUpdate(graph)
        val updates = graph.appUpdates.updates.first()
        when {
            !graph.settings.settings.value.autoUpdateApps || !notifications.enabled -> tellAppUpdates(graph, updates) { count, names ->
                notifications.appUpdates(getPluralString(Res.plurals.notify_updates_available_title, count, count), names)
            }
            else -> {
                // Play Protect stands between an install and the device, never a mount.
                val (asking, silent) = updates.partition { graph.actions.asksBeforeUpdates && it.app.installMethod == InstallMethod.INSTALL }
                update(graph, silent)
                prepare(graph, asking)
            }
        }
        return Result.success()
    }

    private suspend fun tellManagerUpdate(graph: AppGraph) {
        val version = graph.managerUpdates.update.value?.version ?: return
        if (graph.settings.settings.value.notifiedManagerUpdate == version) return
        if (notifications.managerUpdate(getString(Res.string.update_available, version), getString(Res.string.notify_manager_update_body))) {
            graph.settings.update { it.copy(notifiedManagerUpdate = version) }
        }
    }

    /** Tells again only when an app joins the ones already told about; apps that updated are forgotten. */
    private suspend fun tellAppUpdates(graph: AppGraph, updates: List<AppUpdate>, post: suspend (count: Int, names: String) -> Boolean) {
        val packages = updates.map { it.app.packageName }.toSet()
        val told = graph.settings.settings.value.notifiedAppUpdates
        if (packages == told) return
        val joined = (packages - told).isNotEmpty()
        if (joined && !post(updates.size, updates.joinToString { it.app.name })) return
        graph.settings.update { it.copy(notifiedAppUpdates = packages) }
    }

    /** The download is the slow part, so it happens now; patching and installing wait for the tap. */
    private suspend fun prepare(graph: AppGraph, updates: List<AppUpdate>) {
        graph.appUpdater.download(due(updates))
        tellAppUpdates(graph, updates) { count, names ->
            notifications.readyUpdates(getPluralString(Res.plurals.notify_updates_ready_title, count, count), names)
        }
    }

    /** An app the user has open waits for the next run. */
    private suspend fun update(graph: AppGraph, updates: List<AppUpdate>) {
        val due = due(updates)
        graph.appUpdater.run(due)
        val entries = graph.appUpdater.entries.value
        val stopped = due.mapNotNull { (entries[it.app.key]?.status as? UpdateStatus.Failed)?.error }.filter { it !is Failure.AppInUse }
        val updated = due.filter { it.app.key !in entries }
        // The site's check needs a person, so it gets its own words: it's the one stop Manager can't retry alone.
        val body = if (stopped.any { it is Failure.HumanCheckRequired }) Res.string.notify_update_check_body else Res.string.notify_update_failed_body
        when {
            stopped.isNotEmpty() -> notifications.appUpdates(getPluralString(Res.plurals.notify_update_failed_title, stopped.size, stopped.size), getString(body))
            updated.isNotEmpty() -> notifications.appUpdates(getPluralString(Res.plurals.notify_updated_title, updated.size, updated.size), updated.joinToString { it.app.name })
        }
    }

    /** A new app version downloads only on a connection that isn't metered. */
    private fun due(updates: List<AppUpdate>): List<AppUpdate> {
        val metered = checkNotNull(applicationContext.getSystemService<ConnectivityManager>()).isActiveNetworkMetered
        return updates.filter { !metered || it.source !is AppSource.Download }
    }
}
