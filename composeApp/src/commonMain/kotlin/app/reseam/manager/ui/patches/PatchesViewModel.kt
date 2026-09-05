package app.reseam.manager.ui.patches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.sdk.InspectRequest
import app.reseam.manager.sdk.OptionValue
import app.reseam.manager.sdk.ReseamSdk
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PatchesState {
    data object Loading : PatchesState
    data class Failed(val message: String) : PatchesState
    /** [target] carries the package and version the engine read from the APK when the picker did not know them. */
    data class Ready(val editor: PatchEditor, val target: PatchTarget) : PatchesState
}

class PatchesViewModel(private val graph: AppGraph, private val target: PatchTarget) : ViewModel() {
    private val current = MutableStateFlow<PatchesState>(PatchesState.Loading)
    val state: StateFlow<PatchesState> = current.asStateFlow()

    init {
        inspect()
    }

    fun inspect() {
        current.value = PatchesState.Loading
        viewModelScope.launch {
            current.value = try {
                val response = ReseamSdk.inspect(
                    InspectRequest(
                        apkPath = target.apkPath,
                        splitPaths = target.splitPaths,
                        bundlePaths = graph.bundles.paths(),
                        trust = graph.bundles.trust(),
                    ),
                )
                val resolved = target.copy(
                    packageName = target.packageName ?: response.apk?.packageName,
                    versionName = target.versionName ?: response.apk?.versionName,
                )
                PatchesState.Ready(PatchEditor.from(response, resolved.packageName), resolved)
            } catch (error: Exception) {
                PatchesState.Failed(error.userMessage())
            }
        }
    }

    fun toggle(id: String, enabled: Boolean) = edit { it.toggle(id, enabled) }
    fun setOption(id: String, key: String, value: OptionValue) = edit { it.setOption(id, key, value) }
    fun expand(id: String?) = edit { it.expand(id) }
    fun enableAll() = edit { it.enableAll() }
    fun resetDefaults() = inspect()

    private fun edit(transform: (PatchEditor) -> PatchEditor) {
        current.update { state -> if (state is PatchesState.Ready) state.copy(editor = transform(state.editor)) else state }
    }
}
