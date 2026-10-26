package app.reseam.manager.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val DefaultServerUrl = "https://api.reseam.app"

private const val LegacyServerKey = "apiBaseUrl"

@Serializable
data class Settings(
    val serverUrl: String = DefaultServerUrl,
    val autoUpdateBundles: Boolean = true,
    val managerPrereleases: Boolean = false,
    val allowIncompatiblePatches: Boolean = false,
    val useSystemInstaller: Boolean = false,
    val keepDownloads: Boolean = true,
    val mountWithRoot: Boolean = false,
    val askedRunPermissions: Boolean = false,
    val seenAnnouncement: Long = 0,
)

/** Reads settings saved before 0.32, whose `apiBaseUrl` named the server's `/v1` routes instead of the server. */
object SettingsSerializer : JsonTransformingSerializer<Settings>(Settings.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val legacy = element.jsonObject[LegacyServerKey]?.jsonPrimitive?.content ?: return element
        return JsonObject(element.jsonObject - LegacyServerKey + ("serverUrl" to JsonPrimitive(legacy.trimEnd('/').removeSuffix("/v1"))))
    }
}

class SettingsRepository(private val store: JsonStore<Settings>) {
    val settings: StateFlow<Settings> = store.state

    suspend fun update(transform: (Settings) -> Settings) {
        store.update(transform)
    }
}
