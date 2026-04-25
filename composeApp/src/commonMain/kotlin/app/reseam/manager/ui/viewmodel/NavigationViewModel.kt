package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.ui.model.navigation.ManagerRoute

@Stable
class NavigationViewModel internal constructor(
    private val store: ManagerStateStore,
) {
    fun openHome() {
        store.update { it.copy(navigation = it.navigation.reset()) }
    }

    fun openPermissions() {
        store.update { it.copy(navigation = it.navigation.reset(ManagerRoute.Permissions)) }
    }

    fun back() {
        store.update { it.copy(navigation = it.navigation.pop()) }
    }
}
