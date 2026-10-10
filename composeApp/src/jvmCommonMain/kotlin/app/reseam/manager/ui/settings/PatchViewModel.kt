package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.sdk.declared
import app.reseam.manager.ui.lookOf
import app.reseam.sdk.PatchMetadata
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PatchViewModel(private val graph: AppGraph, bundleId: String, patchId: String) : ViewModel() {
    val patch: StateFlow<PatchMetadata?> = graph.bundles.patches
        .map { patches -> patches?.get(bundleId)?.firstOrNull { it.spec.id == patchId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val appNames: StateFlow<Map<String, String>> = patch
        .map { patch -> patch?.declared.orEmpty().associate { it.`package` to graph.lookOf(it.`package`).name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
