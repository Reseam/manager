package app.reseam.manager.ui.app

import androidx.compose.runtime.Composable
import app.reseam.manager.platform.Permission
import app.reseam.manager.platform.rememberPermissions

private val RunPermissions = listOf(Permission.Notifications, Permission.BatteryOptimization)

/** Asks for the permissions a run benefits from, once, at the first Patch. */
@Composable
fun rememberRunPermissionRequest(shouldAsk: () -> Boolean, onAsked: () -> Unit): () -> Unit {
    val permissions = rememberPermissions()
    return {
        if (permissions != null && shouldAsk()) {
            RunPermissions.filterNot { it in permissions.granted }.forEach(permissions::request)
            onAsked()
        }
    }
}
