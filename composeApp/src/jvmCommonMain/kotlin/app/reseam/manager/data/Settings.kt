package app.reseam.manager.data

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val DefaultServerUrl = "https://api.reseam.app"

@Serializable
data class Settings(
    val serverUrl: String = DefaultServerUrl,
    val autoUpdateBundles: Boolean = true,
    val autoUpdateApps: Boolean = false,
    val managerPrereleases: Boolean = false,
    val allowIncompatiblePatches: Boolean = false,
    val installer: Installer = Installer.Android,
    val installAfterPatching: Boolean = false,
    val keepDownloads: Boolean = true,
    val mountWithRoot: Boolean = false,
    val theme: Theme = Theme.System,
    val dynamicColor: Boolean = false,
    /** A BCP 47 tag from `ManagerLanguages`, or null to follow the device. */
    val language: String? = null,
    val askedRunPermissions: Boolean = false,
    val seenAnnouncement: Long = 0,
    /** The Manager version the background check last told about. */
    val notifiedManagerUpdate: String? = null,
    /** The patched apps it last said could update, by package. */
    val notifiedAppUpdates: Set<String> = emptySet(),
)

/** What installs patched apps: Android's own prompt, Shizuku without one, or the user's installer app. */
enum class Installer { Android, Shizuku, OtherApp }

enum class Theme {
    System, Light, Dark;

    fun dark(system: Boolean): Boolean = when (this) {
        System -> system
        Light -> false
        Dark -> true
    }
}

/**
 * Reads settings saved under older keys: before 0.32 `apiBaseUrl` named the server's `/v1` routes instead of the
 * server, and the `useSystemInstaller` switch came before [Installer], sending single APKs to the user's installer app.
 */
object SettingsSerializer : JsonTransformingSerializer<Settings>(Settings.serializer()) {
    private val legacy: Map<String, (JsonPrimitive) -> Pair<String, JsonPrimitive>> = mapOf(
        "apiBaseUrl" to { "serverUrl" to JsonPrimitive(it.content.trimEnd('/').removeSuffix("/v1")) },
        "useSystemInstaller" to { "installer" to JsonPrimitive((if (it.boolean) Installer.OtherApp else Installer.Android).name) },
    )

    override fun transformDeserialize(element: JsonElement): JsonElement = JsonObject(
        legacy.entries.fold(element.jsonObject.toMap()) { settings, (key, migrate) ->
            settings[key]?.let { settings - key + migrate(it.jsonPrimitive) } ?: settings
        },
    )
}

class SettingsRepository(private val store: JsonStore<Settings>) {
    val settings: StateFlow<Settings> = store.state

    suspend fun update(transform: (Settings) -> Settings) {
        store.update(transform)
    }
}
