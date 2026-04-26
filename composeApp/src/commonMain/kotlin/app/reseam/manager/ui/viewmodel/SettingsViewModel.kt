package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.ui.model.SettingsState
import app.reseam.manager.ui.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class SettingsViewModel internal constructor(
    private val store: ManagerStateStore,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    fun setCheckUpdatesDaily(enabled: Boolean) {
        update { it.copy(checkUpdatesDaily = enabled) }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        update { it.copy(analyticsEnabled = enabled) }
    }

    fun setTheme(theme: ThemeMode) {
        update { it.copy(theme = theme) }
    }

    fun setApiBaseUrl(url: String) {
        update { it.copy(apiBaseUrl = url.trim()) }
    }

    fun completeOnboarding() {
        update { it.copy(onboardingCompleted = true) }
    }

    private fun update(change: (SettingsState) -> SettingsState) {
        val next = change(store.state.settings)
        store.update { it.copy(settings = next) }
        scope.launch {
            settings.save(next)
        }
    }
}
