package app.reseam.manager.ui.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

@Composable
fun rememberAndroidPermissionHandler(): PermissionHandler {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val powerManager = remember { context.getSystemService<android.os.PowerManager>() }
    val notifManager = remember { NotificationManagerCompat.from(context) }

    val canInstall = remember { mutableStateOf(pm.canRequestPackageInstalls()) }
    val notifs = remember {
        mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notifManager.areNotificationsEnabled())
    }
    val battery = remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }

    fun doRefresh() {
        canInstall.value = pm.canRequestPackageInstalls()
        notifs.value = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notifManager.areNotificationsEnabled()
        battery.value = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    val installLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { doRefresh() }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { doRefresh() }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { doRefresh() }

    return object : PermissionHandler {
        override val canInstallUnknownApps get() = canInstall.value
        override val isNotificationsEnabled get() = notifs.value
        override val isBatteryOptimizationExempt get() = battery.value

        override fun refresh() = doRefresh()

        override fun requestInstallApps() {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.fromParts("package", context.packageName, null)
            )
            installLauncher.launch(intent)
        }

        override fun requestNotifications() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        override fun requestBatteryOptimization() {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.fromParts("package", context.packageName, null)
            )
            batteryLauncher.launch(intent)
        }
    }
}
