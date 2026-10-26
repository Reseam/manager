package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.ReleaseNote
import app.reseam.manager.data.WhatsNew
import app.reseam.manager.data.fetchManagerReleases
import app.reseam.manager.data.markdown
import app.reseam.manager.data.note
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

internal suspend fun List<ReleaseNote>.entries(): List<ReleaseEntry> = withContext(Dispatchers.Default) {
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

class ReleasesViewModel(private val load: suspend () -> List<ReleaseNote>, private val check: suspend () -> Unit = {}) : ViewModel() {
    private val stateFlow = MutableStateFlow<ReleasesState>(ReleasesState.Loading)
    val state: StateFlow<ReleasesState> = stateFlow.asStateFlow()

    private val checkingState = MutableStateFlow(false)
    val checking: StateFlow<Boolean> = checkingState.asStateFlow()

    init {
        load()
    }

    fun check() {
        if (checkingState.value) return
        viewModelScope.launch {
            checkingState.value = true
            try {
                check.invoke()
            } finally {
                checkingState.value = false
            }
        }
    }

    fun load() {
        stateFlow.value = ReleasesState.Loading
        viewModelScope.launch {
            stateFlow.value = try {
                ReleasesState.Loaded(load.invoke().entries())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ReleasesState.Failed(error)
            }
        }
    }
}

private suspend fun bundleNotes(graph: AppGraph, bundleId: String): List<ReleaseNote> =
    graph.bundles.installed().firstOrNull { it.id == bundleId }?.let { graph.bundles.releaseNotes(it, graph.settings.settings.value.serverUrl) }.orEmpty()

private suspend fun managerNotes(graph: AppGraph): List<ReleaseNote> {
    val settings = graph.settings.settings.value
    return fetchManagerReleases(settings.serverUrl).filter { settings.managerPrereleases || !it.prerelease }.map { it.note() }
}

fun bundleReleases(graph: AppGraph, bundleId: String) = ReleasesViewModel(
    load = { bundleNotes(graph, bundleId) },
    check = { graph.bundles.installed().firstOrNull { it.id == bundleId }?.let { graph.bundleSyncer.check(it).join() } },
)

fun release(graph: AppGraph, bundleId: String?, version: String) = ReleasesViewModel(
    load = { (if (bundleId == null) managerNotes(graph) else bundleNotes(graph, bundleId)).filter { it.version == version } },
)
