package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.ui.model.AppView

@Stable
class NavigationViewModel internal constructor(
    private val store: ManagerStateStore,
) {
    fun back() = store.update {
        if (it.canGoBack) it.copy(backStack = it.backStack.dropLast(1), error = null) else it
    }

    fun openHome() = store.update {
        it.copy(backStack = listOf(AppView.Home), error = null)
    }

    fun openPermissions() = store.update {
        it.copy(backStack = listOf(AppView.Permissions), error = null)
    }

    fun openSettings() = pushUnique(AppView.Settings)

    fun openBundles() = pushUnique(AppView.Bundles)

    fun openAppDetail(appId: String) = pushUnique(AppView.AppDetail(appId))

    private fun pushUnique(target: AppView) = store.update {
        if (it.view == target) it
        else it.copy(backStack = it.backStack + target, error = null)
    }
}
