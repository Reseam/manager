package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.reseam.manager.ui.model.ManagerUiState

@Stable
class ManagerStateStore(
    initialState: ManagerUiState = ManagerUiState(),
) {
    var state by mutableStateOf(initialState)
        private set

    fun update(change: (ManagerUiState) -> ManagerUiState) {
        state = change(state)
    }
}
