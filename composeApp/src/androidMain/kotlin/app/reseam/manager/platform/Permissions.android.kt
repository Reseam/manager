package app.reseam.manager.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.LifecycleResumeEffect

@Composable
actual fun rememberPermissions(): Permissions? {
    val context = LocalContext.current
    val activity = checkNotNull(LocalActivity.current) { "Permissions are asked from an activity" }
    val granted = remember(context) { mutableStateOf(context.grantedPermissions()) }
    val refresh = { granted.value = context.grantedPermissions() }
    val deniedOnce = remember { mutableStateOf(false) }

    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        refresh()
        // Not denied once before the request, and not after it: Android had stopped asking, so only its settings can allow it.
        if (!allowed && !deniedOnce.value && !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            settings.launch(context.notificationSettings())
        }
    }

    LifecycleResumeEffect(context) {
        refresh()
        onPauseOrDispose {}
    }

    return remember(context, settings, notifications) {
        AndroidPermissions(context, activity, granted, deniedOnce, settings, notifications)
    }
}

private class AndroidPermissions(
    private val context: Context,
    private val activity: Activity,
    private val state: State<Set<Permission>>,
    private val deniedOnce: MutableState<Boolean>,
    private val settings: ActivityResultLauncher<Intent>,
    private val notifications: ActivityResultLauncher<String>,
) : Permissions {
    override val granted: Set<Permission> get() = state.value

    override fun request(permission: Permission) = when (permission) {
        Permission.InstallApps -> settings.launch(appSettings(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
        Permission.BatteryOptimization -> settings.launch(appSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
        Permission.Notifications -> requestNotifications()
    }

    /** Before Android 13, or with the permission granted but notifications turned off, only their settings can turn them on. */
    private fun requestNotifications() {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            settings.launch(context.notificationSettings())
            return
        }
        deniedOnce.value = activity.shouldShowRequestPermissionRationale(permission)
        notifications.launch(permission)
    }

    private fun appSettings(action: String) = Intent(action, Uri.fromParts("package", context.packageName, null))
}

private fun Context.notificationSettings() = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

private fun Context.grantedPermissions(): Set<Permission> = buildSet {
    if (packageManager.canRequestPackageInstalls()) add(Permission.InstallApps)
    if (NotificationManagerCompat.from(this@grantedPermissions).areNotificationsEnabled()) add(Permission.Notifications)
    if (getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(packageName) == true) add(Permission.BatteryOptimization)
}
