package app.reseam.manager.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

const val DefaultApiBaseUrl = "https://api.reseam.app/v1"

@Serializable
data class Settings(
    val apiBaseUrl: String = DefaultApiBaseUrl,
    val checkUpdatesDaily: Boolean = true,
    /** Let patches run on app versions they were not declared for. The package check always applies. */
    val allowIncompatiblePatches: Boolean = false,
)

class SettingsRepository(private val store: JsonStore<Settings>) {
    val settings: StateFlow<Settings> = store.state

    suspend fun update(transform: (Settings) -> Settings) {
        store.update(transform)
    }
}
