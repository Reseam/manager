package app.reseam.manager.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

const val DefaultApiBaseUrl = "https://api.reseam.app/v1"

@Serializable
data class Settings(
    val apiBaseUrl: String = DefaultApiBaseUrl,
    val autoUpdateBundles: Boolean = true,
    val allowIncompatiblePatches: Boolean = false,
    val useSystemInstaller: Boolean = false,
    val keepDownloads: Boolean = true,
    val mountWithRoot: Boolean = false,
    val askedRunPermissions: Boolean = false,
    val seenAnnouncement: Long = 0,
)

class SettingsRepository(private val store: JsonStore<Settings>) {
    val settings: StateFlow<Settings> = store.state

    suspend fun update(transform: (Settings) -> Settings) {
        store.update(transform)
    }
}
