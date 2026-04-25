package app.reseam.manager.ui.model

import app.reseam.manager.ui.model.navigation.ManagerRoute
import app.reseam.manager.ui.model.navigation.ManagerNavigationState
import kotlinx.serialization.Serializable

data class ManagerUiState(
    val navigation: ManagerNavigationState = ManagerNavigationState(),
    val home: HomeState = HomeState(),
    val flow: PatchFlowState = PatchFlowState(),
    val bundles: BundlesState = BundlesState(),
    val settings: SettingsState = SettingsState(),
    val busy: Boolean = false,
    val error: String? = null,
) {
    val route: ManagerRoute
        get() = navigation.current
}

sealed interface LoadState<out T> {
    val value: T?
        get() = null

    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Loaded<T>(override val value: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

data class HomeState(
    val patchedApps: List<PatchedAppSummary> = emptyList(),
)

@Serializable
data class PatchedAppSummary(
    val id: String,
    val name: String,
    val packageName: String,
    val versionName: String? = null,
    val patchCount: Int,
    val artifactPath: String,
    val bundleNames: List<String> = emptyList(),
    val update: PatchUpdateInfo? = null,
)

@Serializable
data class PatchUpdateInfo(
    val versionName: String,
    val compatible: Boolean,
)
