package app.reseam.manager.platform

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Bundle
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import app.reseam.manager.R

private const val Channel = "runs"
private const val NotificationId = 1
private const val WakeLockTimeoutMs = 30 * 60 * 1000L

class AndroidBackgroundRun(private val application: Application) : BackgroundRun {
    private var started = 0

    init {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                started++
                NotificationManagerCompat.from(application).cancel(NotificationId)
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

    override suspend fun <T> hold(block: suspend () -> T): T {
        val lock = checkNotNull(application.getSystemService<PowerManager>()).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "reseam:patch")
        lock.acquire(WakeLockTimeoutMs)
        try {
            return block()
        } finally {
            if (lock.isHeld) lock.release()
        }
    }

    override fun finished(title: String, text: String) {
        val notifications = NotificationManagerCompat.from(application)
        if (started > 0 || !notifications.areNotificationsEnabled()) return
        checkNotNull(application.getSystemService<NotificationManager>()).createNotificationChannel(
            NotificationChannel(Channel, application.getString(R.string.notification_channel_runs), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            application,
            0,
            application.packageManager.getLaunchIntentForPackage(application.packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(application, Channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        notifications.notify(NotificationId, notification)
    }
}
