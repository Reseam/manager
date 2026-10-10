package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Bundle
import app.reseam.manager.data.StagedBundle
import app.reseam.manager.ui.SingleTask
import app.reseam.sdk.PatchMetadata
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val BundleOrder = compareByDescending<Bundle> { it.official }.thenBy { it.name.lowercase() }

class SourcesViewModel(private val graph: AppGraph) : ViewModel() {
    val bundles: StateFlow<List<Bundle>?> = combine(graph.bundles.bundles, graph.bundles.patches) { bundles, patches -> bundles.takeIf { patches != null }?.sortedWith(BundleOrder) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val pending: StateFlow<StagedBundle?> = graph.bundles.pending

    private val tasks = SingleTask<Unit>(viewModelScope, graph.notices)
    val busy: StateFlow<Boolean> = combine(tasks.busy, graph.bundles.syncing) { busy, syncing -> busy || syncing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun addUrl(url: String) = tasks.launch(Unit) { graph.bundles.offer(graph.bundles.stageDownload(url.trim())) }

    fun addFile() = tasks.launch(Unit) {
        val picked = FileKit.openFilePicker(type = FileKitType.File(listOf("reseam"))) ?: return@launch
        graph.bundles.offer(graph.bundles.stageFile(picked))
    }

    fun decide(trust: Boolean) = tasks.launch(Unit) { graph.bundles.decide(trust) }
}

class SourceViewModel(private val graph: AppGraph, id: String) : ViewModel() {
    val bundle: StateFlow<Bundle?> = graph.bundles.bundles
        .map { bundles -> bundles.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), graph.bundles.installed().firstOrNull { it.id == id })

    val patches: StateFlow<List<PatchMetadata>?> = graph.bundles.patches
        .map { loaded -> loaded?.get(id)?.filter { !it.spec.hidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val autoUpdates: StateFlow<Boolean> = graph.settings.settings
        .map { it.autoUpdateBundles }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), graph.settings.settings.value.autoUpdateBundles)

    fun remove(onRemoved: () -> Unit) {
        val id = bundle.value?.id ?: return
        viewModelScope.launch { graph.notices.attempt { graph.bundles.remove(id) }?.let { onRemoved() } }
    }
}
