package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.reseam.manager.ui.model.BundlesState
import app.reseam.manager.ui.model.ManagerUiState
import app.reseam.manager.ui.model.PatchFlowState
import app.reseam.manager.ui.model.SettingsState
import app.reseam.manager.ui.model.navigation.ManagerNavigationState

@Stable
class ManagerStateStore(
    initialState: ManagerUiState = ManagerUiState(),
) {
    var state by mutableStateOf(initialState)
        private set

    fun update(change: (ManagerUiState) -> ManagerUiState) {
        state = change(state)
    }

    fun setBusy(busy: Boolean) {
        update { it.copy(busy = busy) }
    }

    fun setError(message: String?) {
        update { it.copy(error = message) }
    }

    fun clearError() {
        setError(null)
    }

    fun navigate(
        clearError: Boolean = true,
        change: (ManagerNavigationState) -> ManagerNavigationState,
    ) {
        update {
            it.copy(
                navigation = change(it.navigation),
                error = if (clearError) null else it.error,
            )
        }
    }

    fun updateFlow(
        clearError: Boolean = false,
        change: (PatchFlowState) -> PatchFlowState,
    ) {
        update {
            it.copy(
                flow = change(it.flow),
                error = if (clearError) null else it.error,
            )
        }
    }

    fun updateBundles(
        clearError: Boolean = false,
        change: (BundlesState) -> BundlesState,
    ) {
        update {
            it.copy(
                bundles = change(it.bundles),
                error = if (clearError) null else it.error,
            )
        }
    }

    fun setSettings(settings: SettingsState) {
        update { it.copy(settings = settings) }
    }
}
