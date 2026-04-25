package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.ui.model.PatchInput
import app.reseam.manager.ui.model.navigation.ManagerRoute

@Stable
class AppDetailViewModel internal constructor(
    private val store: ManagerStateStore,
) {
    fun repatch(appId: String) {
        val app = store.state.home.patchedApps.firstOrNull { it.id == appId } ?: return
        store.update {
            it.copy(
                navigation = it.navigation.setStack(ManagerRoute.Home, ManagerRoute.Inputs, ManagerRoute.Patches),
                flow = it.flow.copy(selectedInput = PatchInput.ApkFile(app.name, app.artifactPath)),
            )
        }
    }
}
