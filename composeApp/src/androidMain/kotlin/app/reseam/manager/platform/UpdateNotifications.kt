package app.reseam.manager.platform

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.reseam.manager.R

/** What the background check found or did, in the Updates channel. Tapping one opens Manager. Posting returns whether it showed. */
class UpdateNotifications(private val context: Context) {
    val enabled: Boolean get() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun managerUpdate(title: String, text: String): Boolean = post(ManagerUpdateNotificationId, title, text, openManager(context))

    /** Patched apps that can update, updated, or couldn't: the newest replaces the one before. */
    fun appUpdates(title: String, text: String): Boolean = post(AppUpdatesNotificationId, title, text, openManager(context))

    /** Updates that wait only for the user: tapping starts them. */
    fun readyUpdates(title: String, text: String): Boolean = post(AppUpdatesNotificationId, title, text, updateApps(context))

    private fun post(id: Int, title: String, text: String, onTap: PendingIntent): Boolean {
        if (!enabled) return false
        val notification = NotificationCompat.Builder(context, UpdatesChannel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(onTap)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
        return true
    }
}
