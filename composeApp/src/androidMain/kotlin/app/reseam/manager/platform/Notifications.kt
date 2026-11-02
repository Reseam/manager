package app.reseam.manager.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.content.getSystemService
import app.reseam.manager.R

internal const val ProgressChannel = "progress"
internal const val ResultChannel = "runs"
internal const val UpdatesChannel = "updates"

internal const val ResultNotificationId = 1
internal const val ProgressNotificationId = 2
internal const val ManagerUpdateNotificationId = 3
internal const val AppUpdatesNotificationId = 4

/** Patching shows progress quietly; results of the user's own runs and updates found in the background can each be muted. */
fun createNotificationChannels(context: Context) {
    checkNotNull(context.getSystemService<NotificationManager>()).createNotificationChannels(
        listOf(
            NotificationChannel(ProgressChannel, context.getString(R.string.notification_channel_progress), NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(ResultChannel, context.getString(R.string.notification_channel_runs), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(UpdatesChannel, context.getString(R.string.notification_channel_updates), NotificationManager.IMPORTANCE_DEFAULT),
        ),
    )
}

/** Opens Manager and starts every update it found. */
const val UpdateAppsAction = "app.reseam.manager.action.UPDATE_APPS"

internal fun updateApps(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)).setAction(UpdateAppsAction),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

internal fun openManager(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    context.packageManager.getLaunchIntentForPackage(context.packageName),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)
