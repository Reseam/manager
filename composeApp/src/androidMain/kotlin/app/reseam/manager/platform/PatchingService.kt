package app.reseam.manager.platform

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.reseam.manager.R

class PatchingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(this, ProgressNotificationId, progressNotification(this, intent?.getStringExtra(TitleExtra).orEmpty()), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) = stopSelf()

    companion object {
        const val TitleExtra = "title"
    }
}

internal fun progressNotification(context: Context, title: String): Notification = NotificationCompat.Builder(context, ProgressChannel)
    .setSmallIcon(R.drawable.ic_launcher_foreground)
    .setContentTitle(title)
    .setProgress(0, 0, true)
    .setOngoing(true)
    .setSilent(true)
    .setContentIntent(openManager(context))
    .build()

internal fun openManager(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    context.packageManager.getLaunchIntentForPackage(context.packageName),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)
