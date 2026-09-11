package app.reseam.manager.ui.nav

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import app.reseam.manager.sdk.PatchMetadata
import app.reseam.manager.sdk.PatchSelection
import app.reseam.manager.ui.components.Icons
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
    @Serializable data class Run(val target: PatchTarget, val selection: PatchSelection, val queue: List<String>, val bundlePaths: List<String>, val patches: List<PatchMetadata> = emptyList()) : Route
    @Serializable data class AppDetail(val packageName: String) : Route
    @Serializable data object Bundles : Route
    @Serializable data class BundleDetail(val id: String) : Route
    @Serializable data object Settings : Route
    @Serializable data object Permissions : Route
}

/** The persistent navigation destinations. Each owns one root route and the routes reached from it. */
enum class Section(val label: String, val icon: ImageVector, val root: Route) {
    Home("Home", Icons.Home, Route.Home),
    Bundles("Bundles", Icons.Puzzle, Route.Bundles),
    Settings("Settings", Icons.Settings, Route.Settings),
}

/** Null for the patch flow, which is a focused task with no navigation beside it. */
val Route.section: Section?
    get() = when (this) {
        Route.Home, is Route.AppDetail -> Section.Home
        Route.Bundles, is Route.BundleDetail -> Section.Bundles
        Route.Settings, Route.Permissions -> Section.Settings
        Route.PickApp, is Route.Patches, is Route.Run -> null
    }
