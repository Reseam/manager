package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Settings
import app.reseam.manager.userMessage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val graph: AppGraph) : ViewModel() {
    val settings: StateFlow<Settings> = graph.settings.settings

    fun setApiBaseUrl(url: String) {
        viewModelScope.launch {
            runCatching { graph.settings.update { it.copy(apiBaseUrl = url.trim().trimEnd('/')) } }
                .onSuccess { graph.syncOfficialBundle(force = true) }
                .onFailure { graph.notices.post(it.userMessage()) }
        }
    }

    fun setCheckUpdatesDaily(enabled: Boolean) = update { it.copy(checkUpdatesDaily = enabled) }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            runCatching { graph.settings.update(transform) }
                .onFailure { graph.notices.post(it.userMessage()) }
        }
    }
}
