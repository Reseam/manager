package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.ui.model.navigation.ManagerRoute

@Stable
class NavigationViewModel internal constructor(
    private val store: ManagerStateStore,
) {
    fun openHome() {
        store.navigate { it.reset() }
    }

    fun openPermissions() {
        store.navigate { it.reset(ManagerRoute.Permissions) }
    }

    fun back() {
        store.navigate { it.pop() }
    }
}
