package app.reseam.manager.ui.patches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Bundle
import app.reseam.manager.sdk.ReseamSdk
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.userMessage
import app.reseam.sdk.InspectRequest
import app.reseam.sdk.InspectResponse
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.OptionValue
import app.reseam.sdk.PatchPreset
import app.reseam.sdk.Problem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An installed bundle the engine could not use for this run. [bundle] is null when the file is not in the library any more. */
data class BundleProblem(val bundle: Bundle?, val fileName: String, val problem: Problem)

sealed interface PatchesState {
    data object Loading : PatchesState
    data class Failed(val message: String) : PatchesState
    /**
     * [target] carries the package and version the engine read from the APK when the picker did not know them, and
     * the install method. [bundlePaths] are the bundles the run may load: the ones without a [BundleProblem].
     * [rootDenied] is set when mounting was asked for and root was refused.
     */
    data class Ready(
        val editor: PatchEditor,
        val target: PatchTarget,
        val response: InspectResponse,
        val problems: List<BundleProblem>,
        val bundlePaths: List<String>,
        val rootDenied: Boolean = false,
    ) : PatchesState
}

class PatchesViewModel(private val graph: AppGraph, private val target: PatchTarget) : ViewModel() {
    val mountAvailable: Boolean = graph.mounter?.available == true

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
                val packageName = target.packageName ?: response.apk?.packageName
                val resolved = target.copy(
                    packageName = packageName,
                    versionName = target.versionName ?: response.apk?.versionName,
                    installMethod = if (mountAvailable && packageName != null && graph.patchedApps.find(packageName).first()?.installMethod == InstallMethod.MOUNT) {
                        InstallMethod.MOUNT
                    } else {
                        target.installMethod
                    },
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

    fun toggle(reference: String, enabled: Boolean) = edit { it.toggle(reference, enabled) }
    fun setOption(reference: String, key: String, value: OptionValue?) = edit { it.setOption(reference, key, value) }
    fun select(reference: String?) = edit { it.select(reference) }
    fun apply(preset: PatchPreset) = edit { it.apply(preset) }

    /** Mounting asks for root first, since the result can only be mounted with it. */
    fun setMount(mount: Boolean) {
        if (!mount) return setInstallMethod(InstallMethod.INSTALL)
        viewModelScope.launch {
            if (graph.mounter?.requestAccess() == true) setInstallMethod(InstallMethod.MOUNT)
            else current.update { state -> if (state is PatchesState.Ready) state.copy(rootDenied = true) else state }
        }
    }

    private fun setInstallMethod(method: InstallMethod) = current.update { state ->
        if (state is PatchesState.Ready) state.copy(target = state.target.copy(installMethod = method), rootDenied = false) else state
    }

    fun updateBundles() {
        current.value = PatchesState.Loading
        viewModelScope.launch {
            graph.syncBundles(force = true).join()
            inspect()
        }
    }

    fun removeBundle(id: String) {
        viewModelScope.launch {
            runCatching { graph.bundles.remove(id) }.onFailure { graph.notices.warn(it.userMessage()) }
            inspect()
        }
    }

    fun allowIncompatible() {
        viewModelScope.launch {
            runCatching { graph.settings.update { it.copy(allowIncompatiblePatches = true) } }
                .onFailure { graph.notices.warn(it.userMessage()) }
        }
    }

    private fun defaults(response: InspectResponse, target: PatchTarget): PatchEditor =
        PatchEditor.from(response, target.packageName, graph.settings.settings.value.allowIncompatiblePatches)

    private fun edit(transform: (PatchEditor) -> PatchEditor) {
        current.update { state -> if (state is PatchesState.Ready) state.copy(editor = transform(state.editor)) else state }
    }
}
