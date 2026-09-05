package app.reseam.manager.ui.bundles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Bundle
import app.reseam.manager.data.StagedBundle
import app.reseam.manager.userMessage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BundlesState(
    val bundles: List<Bundle> = emptyList(),
    val busy: Boolean = false,
    val pendingTrust: StagedBundle? = null,
)

class BundlesViewModel(private val graph: AppGraph) : ViewModel() {
    private val local = MutableStateFlow(BundlesState())

    val state: StateFlow<BundlesState> = combine(local, graph.bundles.bundles, graph.bundles.syncing, graph.bundles.pending) { state, bundles, syncing, pending ->
        state.copy(
            bundles = bundles.sortedWith(compareByDescending<Bundle> { it.official }.thenBy { it.name.lowercase() }),
            busy = state.busy || syncing,
            pendingTrust = pending,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BundlesState())

    fun importUrl(url: String) = stage { graph.bundles.stageDownload(url.trim()) }

    fun importFile() = stage {
        val picked = FileKit.openFilePicker(type = FileKitType.File(listOf("reseam"))) ?: return@stage null
        graph.bundles.stageFile(picked)
    }

    fun refreshOfficial() = graph.syncOfficialBundle(force = true)

    fun decideTrust(trust: Boolean) = busy { graph.bundles.decide(trust) }

    fun remove(id: String) = busy { graph.bundles.remove(id) }

    private fun stage(load: suspend () -> StagedBundle?) = busy {
        val staged = load() ?: return@busy
        graph.bundles.offer(staged)
    }

    private fun busy(block: suspend () -> Unit) {
        if (local.value.busy) return
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (error: Exception) {
                graph.notices.post(error.userMessage())
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }
}

class BundleDetailViewModel(private val graph: AppGraph, id: String) : ViewModel() {
    val bundle: StateFlow<Bundle?> = graph.bundles.bundles
        .map { list -> list.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), graph.bundles.installed().firstOrNull { it.id == id })

    fun remove(onRemoved: () -> Unit) {
        val id = bundle.value?.id ?: return
        viewModelScope.launch {
            runCatching { graph.bundles.remove(id) }
                .onSuccess { onRemoved() }
                .onFailure { graph.notices.post(it.userMessage()) }
        }
    }
}
