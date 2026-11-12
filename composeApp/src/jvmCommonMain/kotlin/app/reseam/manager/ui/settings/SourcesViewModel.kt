package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Bundle
import app.reseam.manager.data.CommunityBundle
import app.reseam.manager.data.CommunityFeed
import app.reseam.manager.data.SourceInput
import app.reseam.manager.data.StagedBundle
import app.reseam.manager.data.bundles
import app.reseam.manager.sdk.matches
import app.reseam.manager.ui.SingleTask
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

    /** Listed sources that aren't added yet; a source is the same one when its key is. */
    val community: StateFlow<CommunitySection> = combine(graph.community.feed, graph.bundles.bundles) { feed, bundles ->
        when (feed) {
            CommunityFeed.Loading -> CommunitySection.Loading
            is CommunityFeed.Failed -> CommunitySection.Failed
            is CommunityFeed.Loaded -> {
                val listed = feed.bundles.filter { it.listed }
                val missing = listed.filter { source -> bundles.none { it.id == source.publicKey } }
                when {
                    listed.isEmpty() -> CommunitySection.Empty
                    missing.isEmpty() -> CommunitySection.AllAdded
                    else -> CommunitySection.Available(missing)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CommunitySection.Loading)

    init {
        refreshCommunity()
    }

    fun refreshCommunity() {
        viewModelScope.launch { graph.community.refresh() }
    }

    fun add() = graph.sourceImports.begin()

    fun review(bundle: CommunityBundle) = graph.sourceImports.open(SourceInput.Link(bundle.indexUrl))

    fun decide(trust: Boolean) = tasks.launch(Unit) { graph.bundles.decide(trust) }
}

sealed interface CommunitySection {
    data object Loading : CommunitySection
    data object Failed : CommunitySection
    data object Empty : CommunitySection
    data object AllAdded : CommunitySection
    data class Available(val bundles: List<CommunityBundle>) : CommunitySection
}

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

    /** Whether Reseam took this source off the community list since it was added. */
    val delisted: StateFlow<Boolean> = graph.community.feed.map { feed -> feed.bundles.any { it.publicKey == id && !it.listed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val queryState = MutableStateFlow("")
    val query: StateFlow<String> = queryState.asStateFlow()

    private val filterState = MutableStateFlow<AppFilter>(AppFilter.All)
    val filter: StateFlow<AppFilter> = filterState.asStateFlow()

    val groups: StateFlow<List<PatchGroup>?> = graph.bundles.patches
        .map { loaded -> loaded?.get(id) }
        .mapLatest { patches -> patches?.let { graph.patchGroups(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val matching: StateFlow<List<PatchGroup>?> = combine(groups, queryState) { groups, query -> groups?.map { group -> group.copy(patches = group.patches.filter { it.text.matches(query) }) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { graph.community.load() }
    }

    fun setQuery(query: String) {
        queryState.value = query
    }

    fun setFilter(filter: AppFilter) {
        filterState.value = filter
    }

    /** Switching moves the source to the newest release of the chosen channel, back to stable when turned off. */
    fun setPrereleases(enabled: Boolean) {
        val id = bundle.value?.id ?: return
        viewModelScope.launch {
            graph.notices.attempt { graph.bundles.setPrereleases(id, enabled) } ?: return@launch
            graph.bundles.installed().firstOrNull { it.id == id }?.let(graph.bundleSyncer::check)
        }
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
