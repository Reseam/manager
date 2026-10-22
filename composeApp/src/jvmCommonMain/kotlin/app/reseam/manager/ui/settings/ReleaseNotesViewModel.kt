package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.ReleaseNote
import app.reseam.manager.data.WhatsNew
import app.reseam.manager.data.markdown
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.parseMarkdown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A release with its notes parsed ahead of composition, so a card lays out at its full height on first frame. */
data class ReleaseEntry(val note: ReleaseNote, val body: State?)

private suspend fun List<ReleaseNote>.entries(): List<ReleaseEntry> = withContext(Dispatchers.Default) {
    map { note -> ReleaseEntry(note, note.markdown().takeIf { it.isNotEmpty() }?.let(::parseMarkdown)) }
}

class WhatsNewViewModel(graph: AppGraph, bundleId: String) : ViewModel() {
    private val bundle = graph.bundles.installed().firstOrNull { it.id == bundleId }
    private val entriesState = MutableStateFlow<List<ReleaseEntry>?>(null)
    val name: String? = bundle?.name
    val whatsNew: WhatsNew? = bundle?.whatsNew
    val entries: StateFlow<List<ReleaseEntry>?> = entriesState.asStateFlow()

    init {
        if (whatsNew != null) {
            viewModelScope.launch { entriesState.value = whatsNew.notes.filter { it.description.isNotBlank() }.entries() }
            viewModelScope.launch { graph.notices.attempt { graph.bundles.dismissWhatsNew(bundleId) } }
        }
    }
}

sealed interface ReleasesState {
    data object Loading : ReleasesState
    data class Loaded(val entries: List<ReleaseEntry>) : ReleasesState
    data class Failed(val error: Exception) : ReleasesState
}

class ReleasesViewModel(private val graph: AppGraph, private val bundleId: String) : ViewModel() {
    private val stateFlow = MutableStateFlow<ReleasesState>(ReleasesState.Loading)
    val state: StateFlow<ReleasesState> = stateFlow.asStateFlow()

    val checking: StateFlow<Boolean> = graph.bundles.syncing

    fun check() {
        graph.bundles.installed().firstOrNull { it.id == bundleId }?.let(graph.bundleSyncer::check)
    }

    init {
        load()
    }

    fun load() {
        val bundle = graph.bundles.installed().firstOrNull { it.id == bundleId } ?: return
        stateFlow.value = ReleasesState.Loading
        viewModelScope.launch {
            stateFlow.value = try {
                ReleasesState.Loaded(graph.bundles.releaseNotes(bundle, graph.settings.settings.value.apiBaseUrl).entries())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ReleasesState.Failed(error)
            }
        }
    }
}
