package app.reseam.manager.ui.model

import app.reseam.manager.patcher.InspectResponse

data class PatchFlowState(
    val inputMode: InputMode = InputMode.Installed,
    val searchQuery: String = "",
    val installedApps: List<InstalledAppSummary> = emptyList(),
    val selectedInput: PatchInput? = null,
    val inspect: LoadState<InspectResponse> = LoadState.Idle,
    val editor: PatchEditorState = PatchEditorState(),
    val run: PatchRunState = PatchRunState(),
) {
    val filteredInstalledApps: List<InstalledAppSummary>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return installedApps
            return installedApps.filter {
                it.name.lowercase().contains(query) ||
                    it.packageName.lowercase().contains(query)
            }
        }

    val canContinueFromInputs: Boolean
        get() = selectedInput != null && inspect !is LoadState.Loading
}

enum class InputMode {
    Installed,
    File,
}

sealed interface PatchInput {
    val displayName: String
    val apkPath: String
    val splitPaths: List<String>

    data class InstalledApp(
        val app: InstalledAppSummary,
    ) : PatchInput {
        override val displayName: String = app.name
        override val apkPath: String = app.apkPath
        override val splitPaths: List<String> = app.splitPaths
    }

    data class ApkFile(
        override val displayName: String,
        override val apkPath: String,
        override val splitPaths: List<String> = emptyList(),
    ) : PatchInput
}

data class InstalledAppSummary(
    val id: String,
    val name: String,
    val packageName: String,
    val versionName: String? = null,
    val sizeBytes: Long? = null,
    val apkPath: String,
    val splitPaths: List<String> = emptyList(),
    val compatiblePatchCount: Int? = null,
) {
    val hasCompatiblePatches: Boolean
        get() = compatiblePatchCount != 0
}
