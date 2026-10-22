package app.reseam.manager.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

data class InstallResult(val status: Int, val message: String?, val packageName: String?, val prompted: Boolean)

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val action = intent.action ?: return
        val completion = outcomes[action] ?: return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            if (confirm != null && context != null) {
                prompted += action
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return
        }
        outcomes.remove(action)
        completion.complete(
            InstallResult(
                status = status,
                message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE),
                packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME),
                prompted = prompted.remove(action),
            ),
        )
    }

    companion object {
        val outcomes = ConcurrentHashMap<String, CompletableDeferred<InstallResult>>()
        val prompted: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }
}
