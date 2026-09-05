package app.reseam.manager.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

const val DefaultApiBaseUrl = "https://api.reseam.app/v1"

@Serializable
data class Settings(
    val apiBaseUrl: String = DefaultApiBaseUrl,
    val checkUpdatesDaily: Boolean = true,
)

class SettingsRepository(private val store: JsonStore<Settings>) {
    val settings: StateFlow<Settings> = store.state

    suspend fun update(transform: (Settings) -> Settings) {
        store.update(transform)
    }
}
