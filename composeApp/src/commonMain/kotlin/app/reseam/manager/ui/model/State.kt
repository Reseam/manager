package app.reseam.manager.ui.model

import kotlinx.serialization.Serializable

data class ManagerUiState(
    val backStack: List<AppView> = listOf(AppView.Home),
    val home: HomeState = HomeState(),
    val bundles: BundlesState = BundlesState(),
    val settings: SettingsState = SettingsState(),
    val busy: Boolean = false,
    val error: String? = null,
) {
    val view: AppView
        get() = backStack.last()

    val canGoBack: Boolean
        get() = backStack.size > 1 && view.allowsBack
}

data class HomeState(
    val patchedApps: List<PatchedAppSummary> = emptyList(),
    val installedApps: List<InstalledAppSummary> = emptyList(),
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
