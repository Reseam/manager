package app.reseam.manager.ui.model

import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.patcher.OptionKind
import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.patcher.PatchSelection

data class PatchEditorState(
    val packageName: String? = null,
    val cachedPatches: List<PatchMetadata> = emptyList(),
    val inspect: app.reseam.manager.patcher.InspectResponse? = null,
    val patches: List<PatchEditorItem> = emptyList(),
    val openPatchName: String? = null,
) {
    val activeCount: Int
        get() = patches.count { it.enabled }

    val canPatch: Boolean
        get() = patches.any { it.enabled }

    fun toSelection(): PatchSelection {
        val enabled = patches.filter { it.enabled }.map { it.metadata.name }
        val disabled = patches.filterNot { it.enabled }.map { it.metadata.name }
        val options = patches.associate { patch ->
            patch.metadata.name to patch.options.mapValues { it.value.value }
        }
        return PatchSelection(enable = enabled, disable = disabled, options = options)
    }
}

data class PatchEditorItem(
    val metadata: PatchMetadata,
    val enabled: Boolean,
    val required: Boolean = false,
    val options: Map<String, PatchOptionEditorValue> = emptyMap(),
) {
    val optionCount: Int
        get() = metadata.options.size
}

data class PatchOptionEditorValue(
    val key: String,
    val kind: OptionKind,
    val value: InputOptionValue,
    val validValues: List<String> = emptyList(),
)
