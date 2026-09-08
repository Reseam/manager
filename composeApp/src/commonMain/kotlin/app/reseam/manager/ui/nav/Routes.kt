package app.reseam.manager.ui.nav

import androidx.navigation3.runtime.NavKey
import app.reseam.manager.sdk.PatchSelection
import kotlinx.serialization.Serializable

/** The APK a patch run works on. Carried in route keys so each step's state survives process death. */
@Serializable
data class PatchTarget(
    val name: String,
    val packageName: String?,
    val versionName: String?,
    val apkPath: String,
    val splitPaths: List<String> = emptyList(),
    val iconPath: String? = null,
)

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Home : Route
    @Serializable data object PickApp : Route
    @Serializable data class Patches(val target: PatchTarget) : Route
    @Serializable data class Run(val target: PatchTarget, val selection: PatchSelection, val queue: List<String>) : Route
    @Serializable data class AppDetail(val packageName: String) : Route
    @Serializable data object Bundles : Route
    @Serializable data class BundleDetail(val id: String) : Route
    @Serializable data object Settings : Route
    @Serializable data object Permissions : Route
}

val Route.depth: Int
    get() = when (this) {
        Route.Home -> 0
        Route.PickApp, Route.Bundles, Route.Settings, Route.Permissions, is Route.AppDetail -> 1
        is Route.Patches, is Route.BundleDetail -> 2
        is Route.Run -> 3
    }
