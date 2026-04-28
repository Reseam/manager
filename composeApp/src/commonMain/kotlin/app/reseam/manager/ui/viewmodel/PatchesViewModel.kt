package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.ui.model.PatchEditorItem
import app.reseam.manager.ui.model.PatchEditorState
import app.reseam.manager.ui.model.PatchEditorFactory

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

    fun resetDefaults() {
        val inspect = store.state.flow.inspect.value ?: return
        updateEditor { editor ->
            PatchEditorFactory.create(
                appName = editor.appName ?: store.state.flow.selectedInput?.displayName,
                inspect = inspect,
                packageName = editor.packageName,
                cachedPatches = editor.cachedPatches,
            )
        }
    }

    private fun updateEditor(transform: (PatchEditorState) -> PatchEditorState) {
        store.updateFlow { it.copy(editor = transform(it.editor)) }
    }

    private fun PatchEditorState.mapPatches(block: (PatchEditorItem) -> PatchEditorItem): PatchEditorState =
        copy(patches = patches.map(block))
}
