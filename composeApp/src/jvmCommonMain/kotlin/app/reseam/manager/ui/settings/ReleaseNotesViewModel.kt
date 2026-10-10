package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.ReleaseNote
import app.reseam.manager.data.WhatsNew
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WhatsNewViewModel(graph: AppGraph, bundleId: String) : ViewModel() {
    private val bundle = graph.bundles.installed().firstOrNull { it.id == bundleId }
    val name: String? = bundle?.name
    val whatsNew: WhatsNew? = bundle?.whatsNew

    init {
        if (whatsNew != null) viewModelScope.launch { graph.notices.attempt { graph.bundles.dismissWhatsNew(bundleId) } }
    }
}

sealed interface ReleasesState {
    data object Loading : ReleasesState
    data class Loaded(val notes: List<ReleaseNote>) : ReleasesState
    data class Failed(val error: Exception) : ReleasesState
}

class ReleasesViewModel(private val graph: AppGraph, private val bundleId: String) : ViewModel() {
    private val stateFlow = MutableStateFlow<ReleasesState>(ReleasesState.Loading)
    val state: StateFlow<ReleasesState> = stateFlow.asStateFlow()

    init {
        load()
    }

    fun load() {
        val bundle = graph.bundles.installed().firstOrNull { it.id == bundleId } ?: return
        stateFlow.value = ReleasesState.Loading
        viewModelScope.launch {
            stateFlow.value = try {
                ReleasesState.Loaded(graph.bundles.releaseNotes(bundle, graph.settings.settings.value.apiBaseUrl))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ReleasesState.Failed(error)
            }
        }
    }
}
