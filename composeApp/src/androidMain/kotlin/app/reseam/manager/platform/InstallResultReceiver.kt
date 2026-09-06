package app.reseam.manager.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

/** Receives the PackageInstaller session result; explicit component target of the mutable status PendingIntent. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val completion = outcomes[intent.action] ?: return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            if (confirm != null && context != null) {
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return
        }
        outcomes.remove(intent.action)
        completion.complete(status to intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
    }

    companion object {
        val outcomes = ConcurrentHashMap<String, CompletableDeferred<Pair<Int, String?>>>()
    }
}
