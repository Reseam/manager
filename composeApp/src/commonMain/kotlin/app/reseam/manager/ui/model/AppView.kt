package app.reseam.manager.ui.model

sealed interface AppView {
    data object Home : AppView
    data class AppDetail(val appId: String) : AppView
    data object Bundles : AppView
    data class BundleDetail(val bundleId: String) : AppView
    data object Settings : AppView
    data object Permissions : AppView

    sealed interface Flow : AppView {
        data class Choosing(
            val mode: InputMode = InputMode.Installed,
            val query: String = "",
            val selected: PatchInput? = null,
            val inspecting: Boolean = false,
        ) : Flow

        data class Editing(
            val input: PatchInput,
            val editor: PatchEditorState,
        ) : Flow

        data class Running(
            val input: PatchInput,
            val editor: PatchEditorState,
            val run: PatchRunState,
        ) : Flow
    }

    val allowsBack: Boolean
        get() = when (this) {
            is Flow.Running -> run.status != RunStatus.Running
            else -> true
        }
}
