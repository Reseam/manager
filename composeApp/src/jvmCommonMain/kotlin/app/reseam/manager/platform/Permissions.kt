package app.reseam.manager.platform

import androidx.compose.runtime.Composable

enum class Permission { InstallApps, Notifications, BatteryOptimization, Shizuku }

interface Permissions {
    val granted: Set<Permission>

    fun request(permission: Permission)
}

@Composable
expect fun rememberPermissions(): Permissions?
