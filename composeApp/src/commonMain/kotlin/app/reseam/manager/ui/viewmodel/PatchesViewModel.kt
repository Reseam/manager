package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.ui.model.AppView
import app.reseam.manager.ui.model.PatchEditorFactory
import app.reseam.manager.ui.model.PatchEditorItem
import app.reseam.manager.ui.model.PatchEditorState

@Stable
class PatchesViewModel internal constructor(
    private val store: ManagerStateStore,
) {
    fun togglePatch(patchName: String, enabled: Boolean) = updateEditor { editor ->
        editor.mapPatches { patch ->
            if (patch.metadata.name == patchName && !patch.required) patch.copy(enabled = enabled) else patch
        }
    }

    fun openOptions(patchName: String?) = updateEditor { it.copy(openPatchName = patchName) }

    fun updateOption(patchName: String, optionKey: String, value: InputOptionValue) = updateEditor { editor ->
        editor.mapPatches { patch ->
            if (patch.metadata.name != patchName) return@mapPatches patch
            patch.copy(
                options = patch.options.mapValues { (key, old) -> if (key == optionKey) old.copy(value = value) else old },
            )
        }
    }

    fun selectAll() = updateEditor { editor ->
        editor.mapPatches { it.copy(enabled = it.metadata.isCompatible) }
    }

    fun resetDefaults() = updateEditor { editor ->
        val inspect = editor.inspect ?: return@updateEditor editor
        PatchEditorFactory.create(
            inspect = inspect,
            packageName = editor.packageName,
            cachedPatches = editor.cachedPatches,
        )
    }

    private fun updateEditor(transform: (PatchEditorState) -> PatchEditorState) {
        store.update {
            val editing = it.view as? AppView.Flow.Editing ?: return@update it
            it.copy(backStack = it.backStack.dropLast(1) + editing.copy(editor = transform(editing.editor)))
        }
    }

    private fun PatchEditorState.mapPatches(block: (PatchEditorItem) -> PatchEditorItem): PatchEditorState =
        copy(patches = patches.map(block))
}
