package app.reseam.manager.platform

import androidx.compose.runtime.Composable

enum class Permission { InstallApps, Notifications, BatteryOptimization }

/** System grants this platform gates patching behind. */
interface Permissions {
    val granted: Set<Permission>

    fun request(permission: Permission)
}

val Permissions.allGranted: Boolean
    get() = granted.size == Permission.entries.size

/** Bound to the current activity, so it lives in the composition. Null where nothing is gated. */
@Composable
expect fun rememberPermissions(): Permissions?
