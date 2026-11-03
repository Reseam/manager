package app.reseam.manager.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

/** [legacyStatus] is the `INSTALL_FAILED_*` code behind [status], which folds several causes into one. */
data class InstallResult(val status: Int, val legacyStatus: Int, val message: String?, val packageName: String?, val prompted: Boolean)

/** `PackageInstaller.EXTRA_LEGACY_STATUS`, hidden but sent with every result since Android 5. */
private const val ExtraLegacyStatus = "android.content.pm.extra.LEGACY_STATUS"

/** `PackageManager.INSTALL_FAILED_VERSION_DOWNGRADE`. */
const val InstallFailedVersionDowngrade = -25

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
                legacyStatus = intent.getIntExtra(ExtraLegacyStatus, 0),
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
