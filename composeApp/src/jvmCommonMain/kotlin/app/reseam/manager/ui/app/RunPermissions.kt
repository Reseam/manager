package app.reseam.manager.ui.app

import androidx.compose.runtime.Composable
import app.reseam.manager.platform.Permission
import app.reseam.manager.platform.rememberPermissions

@Composable
fun rememberRunPermissionRequest(shouldAsk: () -> Boolean, onAsked: () -> Unit): () -> Unit {
    val permissions = rememberPermissions()
    return {
        if (permissions != null && shouldAsk()) {
            if (Permission.Notifications !in permissions.granted) permissions.request(Permission.Notifications)
            onAsked()
        }
    }
}
