package app.reseam.manager.ui.patches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Bundle
import app.reseam.manager.sdk.InspectRequest
import app.reseam.manager.sdk.InspectResponse
import app.reseam.manager.sdk.OptionValue
import app.reseam.manager.sdk.Problem
import app.reseam.manager.sdk.ReseamSdk
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An installed bundle the engine could not use for this run. [bundle] is null when the file is not in the library any more. */
data class BundleProblem(val bundle: Bundle?, val fileName: String, val problem: Problem)

sealed interface PatchesState {
    data object Loading : PatchesState
    data class Failed(val message: String) : PatchesState
    /**
     * [target] carries the package and version the engine read from the APK when the picker did not know them.
     * [bundlePaths] are the bundles the run may load: the ones without a [BundleProblem].
     */
    data class Ready(
        val editor: PatchEditor,
        val target: PatchTarget,
        val response: InspectResponse,
        val problems: List<BundleProblem>,
        val bundlePaths: List<String>,
    ) : PatchesState
}

class PatchesViewModel(private val graph: AppGraph, private val target: PatchTarget) : ViewModel() {
    private val current = MutableStateFlow<PatchesState>(PatchesState.Loading)
    val state: StateFlow<PatchesState> = current.asStateFlow()

    init {
        inspect()
        viewModelScope.launch {
            graph.settings.settings.map { it.allowIncompatiblePatches }.distinctUntilChanged().collect { allowed -> edit { it.allow(allowed) } }
        }
    }

    fun inspect() {
        current.value = PatchesState.Loading
        viewModelScope.launch {
            current.value = try {
                val installed = graph.bundles.installed()
                val response = ReseamSdk.inspect(
                    InspectRequest(
                        apkPath = target.apkPath,
                        splitPaths = target.splitPaths,
                        bundlePaths = installed.map { it.path },
                        trust = graph.bundles.trust(),
                    ),
                )
                val resolved = target.copy(
                    packageName = target.packageName ?: response.apk?.packageName,
                    versionName = target.versionName ?: response.apk?.versionName,
                )
                val byBundle = installed.zip(response.bundles)
                PatchesState.Ready(
                    editor = defaults(response, resolved),
                    target = resolved,
                    response = response,
                    problems = byBundle.mapNotNull { (bundle, meta) -> meta.problem?.let { BundleProblem(bundle, meta.fileName, it) } },
                    bundlePaths = byBundle.filter { (_, meta) -> meta.problem == null }.map { (bundle, _) -> bundle.path },
                )
            } catch (error: Exception) {
                PatchesState.Failed(error.userMessage())
            }
        }
    }

    fun toggle(id: String, enabled: Boolean) = edit { it.toggle(id, enabled) }
    fun setOption(id: String, key: String, value: OptionValue) = edit { it.setOption(id, key, value) }
    fun select(id: String?) = edit { it.select(id) }
    fun enableAll() = edit { it.enableAll() }

    fun resetDefaults() {
        current.update { state -> if (state is PatchesState.Ready) state.copy(editor = defaults(state.response, state.target).select(state.editor.selected)) else state }
    }

    fun updateOfficialBundle() {
        current.value = PatchesState.Loading
        viewModelScope.launch {
            graph.syncOfficialBundle(force = true).join()
            inspect()
        }
    }

    fun removeBundle(id: String) {
        viewModelScope.launch {
            runCatching { graph.bundles.remove(id) }.onFailure { graph.notices.post(it.userMessage()) }
            inspect()
        }
    }

    fun allowIncompatible() {
        viewModelScope.launch {
            runCatching { graph.settings.update { it.copy(allowIncompatiblePatches = true) } }
                .onFailure { graph.notices.post(it.userMessage()) }
        }
    }

    private fun defaults(response: InspectResponse, target: PatchTarget): PatchEditor =
        PatchEditor.from(response, target.packageName, graph.settings.settings.value.allowIncompatiblePatches)

    private fun edit(transform: (PatchEditor) -> PatchEditor) {
        current.update { state -> if (state is PatchesState.Ready) state.copy(editor = transform(state.editor)) else state }
    }
}
