package app.reseam.manager.ui.nav

import androidx.navigation3.runtime.NavKey
import app.reseam.manager.data.AppSource
import app.reseam.manager.sdk.SelectionSerializer
import app.reseam.sdk.PatchSelection
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Home : Route

    @Serializable data class App(val packageName: String, val picked: AppSource? = null) : Route

    @Serializable data class Patches(val packageName: String) : Route

    @Serializable
    data class Run(
        val packageName: String,
        val source: AppSource,
        @Serializable(with = SelectionSerializer::class) val selection: PatchSelection,
        val mount: Boolean,
        val startedAtEpochMs: Long,
    ) : Route

    @Serializable data object Settings : Route

    @Serializable data object Sources : Route

    @Serializable data class Source(val id: String) : Route

    @Serializable data class Patch(val bundleId: String, val patchId: String) : Route

    @Serializable data object SigningKey : Route

    @Serializable data object SavedApks : Route

    @Serializable data object Announcements : Route

    @Serializable data class Announcement(val id: Long) : Route

    @Serializable data class WhatsNew(val bundleId: String) : Route

    @Serializable data class Releases(val bundleId: String) : Route
}

val Route.packageName: String?
    get() = when (this) {
        is Route.App -> packageName
        is Route.Patches -> packageName
        else -> null
    }
