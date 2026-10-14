package app.reseam.manager.platform

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import app.reseam.manager.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val ResultChannel = "runs"
internal const val ProgressChannel = "progress"
private const val ResultNotificationId = 1
internal const val ProgressNotificationId = 2
private const val WakeLockTimeoutMs = 30 * 60 * 1000L

class AndroidBackgroundRun(private val application: Application) : BackgroundRun {
    private var started = 0

    init {
        checkNotNull(application.getSystemService<NotificationManager>()).createNotificationChannels(
            listOf(
                NotificationChannel(ProgressChannel, application.getString(R.string.notification_channel_progress), NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(ResultChannel, application.getString(R.string.notification_channel_runs), NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
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

    override suspend fun <T> hold(title: Flow<String>, block: suspend () -> T): T = coroutineScope {
        val service = Intent(application, PatchingService::class.java).putExtra(PatchingService.TitleExtra, title.first())
        ContextCompat.startForegroundService(application, service)
        val retitle = launch {
            val notifications = NotificationManagerCompat.from(application)
            title.drop(1).collect { if (notifications.areNotificationsEnabled()) notifications.notify(ProgressNotificationId, progressNotification(application, it)) }
        }
        val lock = checkNotNull(application.getSystemService<PowerManager>()).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reseam:patch")
        lock.acquire(WakeLockTimeoutMs)
        try {
            block()
        } finally {
            retitle.cancel()
            if (lock.isHeld) lock.release()
            application.stopService(service)
        }
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
