package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Bundle
import app.reseam.manager.data.StagedBundle
import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.matches
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.SingleTask
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.lookOf
import app.reseam.sdk.PatchMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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

    fun add() = graph.sourceImports.begin()

    fun decide(trust: Boolean) = tasks.launch(Unit) { graph.bundles.decide(trust) }
}

data class PatchGroup(val app: AppLook?, val patches: List<PatchMetadata>)

sealed interface AppFilter {
    data object All : AppFilter

    data class Only(val packageName: String?) : AppFilter
}

fun AppFilter.shows(group: PatchGroup): Boolean = this == AppFilter.All || this == AppFilter.Only(group.app?.packageName)

@OptIn(ExperimentalCoroutinesApi::class)
class SourceViewModel(private val graph: AppGraph, id: String) : ViewModel() {
    val bundle: StateFlow<Bundle?> = graph.bundles.bundles
        .map { bundles -> bundles.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), graph.bundles.installed().firstOrNull { it.id == id })

    private val queryState = MutableStateFlow("")
    val query: StateFlow<String> = queryState.asStateFlow()

    private val filterState = MutableStateFlow<AppFilter>(AppFilter.All)
    val filter: StateFlow<AppFilter> = filterState.asStateFlow()

    val groups: StateFlow<List<PatchGroup>?> = graph.bundles.patches
        .map { loaded -> loaded?.get(id)?.filter { !it.spec.hidden } }
        .mapLatest { patches ->
            patches ?: return@mapLatest null
            val byApp = patches.flatMap { patch -> patch.declared.map { it.`package` to patch } }
                .groupBy({ it.first }, { it.second })
                .map { (packageName, list) -> PatchGroup(graph.lookOf(packageName), list.distinct()) }
                .sortedByDescending { it.patches.size }
            byApp + listOfNotNull(patches.filter { it.universal }.takeIf { it.isNotEmpty() }?.let { PatchGroup(null, it) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val matching: StateFlow<List<PatchGroup>?> = combine(groups, queryState) { groups, query -> groups?.map { group -> group.copy(patches = group.patches.filter { it.matches(query) }) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(query: String) {
        queryState.value = query
    }

    fun setFilter(filter: AppFilter) {
        filterState.value = filter
    }

    fun setEnabled(enabled: Boolean) {
        val id = bundle.value?.id ?: return
        viewModelScope.launch {
            graph.notices.attempt { graph.bundles.setEnabled(id, enabled) }
            if (enabled) graph.bundleSyncer.sync()
        }
    }

    /** The page closes itself once the source is gone. */
    fun remove() {
        val id = bundle.value?.id ?: return
        viewModelScope.launch { graph.notices.attempt { graph.bundles.remove(id) } }
    }
}
