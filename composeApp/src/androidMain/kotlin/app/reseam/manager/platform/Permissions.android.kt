package app.reseam.manager.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
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
    val granted = remember(context) { mutableStateOf(context.grantedPermissions()) }
    val refresh = { granted.value = context.grantedPermissions() }

    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh() }

    // Both settings screens leave the app, so the grant only shows up once we are back.
    LifecycleResumeEffect(context) {
        refresh()
        onPauseOrDispose {}
    }

    return remember(context, settings, notifications) {
        AndroidPermissions(context, granted, settings, notifications)
    }
}

private class AndroidPermissions(
    private val context: Context,
    private val state: State<Set<Permission>>,
    private val settings: ActivityResultLauncher<Intent>,
    private val notifications: ActivityResultLauncher<String>,
) : Permissions {
    override val granted: Set<Permission> get() = state.value

    override fun request(permission: Permission) = when (permission) {
        Permission.InstallApps -> settings.launch(appSettings(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
        Permission.BatteryOptimization -> settings.launch(appSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS))
        Permission.Notifications -> notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun appSettings(action: String) = Intent(action, Uri.fromParts("package", context.packageName, null))
}

private fun Context.grantedPermissions(): Set<Permission> = buildSet {
    if (packageManager.canRequestPackageInstalls()) add(Permission.InstallApps)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || NotificationManagerCompat.from(this@grantedPermissions).areNotificationsEnabled()) {
        add(Permission.Notifications)
    }
    if (getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(packageName) == true) add(Permission.BatteryOptimization)
}
