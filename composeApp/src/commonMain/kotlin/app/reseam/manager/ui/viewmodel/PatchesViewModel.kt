package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.ui.model.PatchEditorFactory

@Stable
class PatchesViewModel internal constructor(
    private val store: ManagerStateStore,
) {
    fun togglePatch(patchName: String, enabled: Boolean) {
        val editor = store.state.flow.editor
        store.update {
            it.copy(
                flow = it.flow.copy(
                    editor = editor.copy(
                        patches = editor.patches.map { patch ->
                            if (patch.metadata.name == patchName && !patch.required) {
                                patch.copy(enabled = enabled)
                            } else {
                                patch
                            }
                        },
                    ),
                ),
            )
        }
    }

    fun openOptions(patchName: String?) {
        store.update {
            it.copy(flow = it.flow.copy(editor = it.flow.editor.copy(openPatchName = patchName)))
        }
    }

    fun updateOption(patchName: String, optionKey: String, value: InputOptionValue) {
        val editor = store.state.flow.editor
        store.update {
            it.copy(
                flow = it.flow.copy(
                    editor = editor.copy(
                        patches = editor.patches.map { patch ->
                            if (patch.metadata.name != patchName) return@map patch
                            patch.copy(
                                options = patch.options.mapValues { (key, old) ->
                                    if (key == optionKey) old.copy(value = value) else old
                                },
                            )
                        },
                    ),
                ),
            )
        }
    }

    fun selectAll() {
        val editor = store.state.flow.editor
        store.update {
            it.copy(
                flow = it.flow.copy(
                    editor = editor.copy(patches = editor.patches.map { patch ->
                        patch.copy(enabled = patch.metadata.isCompatible)
                    }),
                ),
            )
        }
    }

    fun resetDefaults() {
        val inspect = store.state.flow.inspect.value ?: return
        store.update {
            it.copy(
                flow = it.flow.copy(
                    editor = PatchEditorFactory.create(it.flow.selectedInput?.displayName, inspect),
                ),
            )
        }
    }
}
