package app.reseam.manager.platform

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import app.reseam.manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val WakeLockTimeoutMs = 30 * 60 * 1000L

class AndroidBackgroundRun(private val application: Application) : BackgroundRun {
    private var started = 0

    /** The title of each hold in progress, oldest first; the newest shows. Touched on the main thread only. */
    private val titles = LinkedHashMap<Any, String>()

    private val lock = checkNotNull(application.getSystemService<PowerManager>()).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reseam:patch")

    init {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                started++
                NotificationManagerCompat.from(application).cancel(ResultNotificationId)
            }

            override fun onActivityStopped(activity: Activity) {
                started--
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    /** Holds overlap (a patch run while apps update): they share the service, and it stops when the last one ends. */
    override suspend fun <T> hold(title: Flow<String>, block: suspend () -> T): T = coroutineScope {
        val key = Any()
        val first = title.first()
        withContext(Dispatchers.Main) { enter(key, first) }
        val retitle = launch(Dispatchers.Main) { title.drop(1).collect { retitle(key, it) } }
        try {
            block()
        } finally {
            retitle.cancel()
            withContext(NonCancellable + Dispatchers.Main) { leave(key) }
        }
    }

    private fun enter(key: Any, title: String) {
        titles[key] = title
        if (titles.size == 1) {
            ContextCompat.startForegroundService(application, Intent(application, PatchingService::class.java).putExtra(PatchingService.TitleExtra, title))
            lock.acquire(WakeLockTimeoutMs)
        } else {
            show(title)
        }
    }

    private fun retitle(key: Any, title: String) {
        titles[key] = title
        if (titles.keys.last() == key) show(title)
    }

    private fun leave(key: Any) {
        titles.remove(key)
        if (titles.isEmpty()) {
            if (lock.isHeld) lock.release()
            application.stopService(Intent(application, PatchingService::class.java))
        } else {
            show(titles.values.last())
        }
    }

    private fun show(title: String) {
        val notifications = NotificationManagerCompat.from(application)
        if (notifications.areNotificationsEnabled()) notifications.notify(ProgressNotificationId, progressNotification(application, title))
    }

    override fun finished(title: String, text: String) {
        val notifications = NotificationManagerCompat.from(application)
        if (started > 0 || !notifications.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(application, ResultChannel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openManager(application))
            .setAutoCancel(true)
            .build()
        notifications.notify(ResultNotificationId, notification)
    }
}
