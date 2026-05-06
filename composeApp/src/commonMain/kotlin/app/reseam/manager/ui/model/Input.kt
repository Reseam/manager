package app.reseam.manager.ui.model

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

fun List<InstalledAppSummary>.matching(query: String): List<InstalledAppSummary> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return this
    return filter { it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
}
