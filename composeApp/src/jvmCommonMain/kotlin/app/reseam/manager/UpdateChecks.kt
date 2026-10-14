package app.reseam.manager

import app.reseam.manager.data.AnnouncementRepository
import app.reseam.manager.data.BundleSyncer
import app.reseam.manager.data.ManagerUpdater
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val ForegroundInterval = 1.hours

@OptIn(ExperimentalTime::class)
class UpdateChecks(
    private val scope: CoroutineScope,
    private val announcements: AnnouncementRepository,
    private val managerUpdates: ManagerUpdater,
    private val bundleSyncer: BundleSyncer,
) {
    private var checkedAt: Instant? = null

    fun onForeground() {
        val now = Clock.System.now()
        if (checkedAt?.let { now - it < ForegroundInterval } == true) return
        checkedAt = now
        scope.launch { check() }
    }

    suspend fun refresh() {
        checkedAt = Clock.System.now()
        scope.launch { check() }.join()
    }

    private suspend fun check() = coroutineScope {
        launch { announcements.refresh() }
        if (managerUpdates.check() == null) bundleSyncer.sync().join()
    }
}
