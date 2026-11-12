package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.CommunityBundle
import app.reseam.manager.data.SourceImport
import app.reseam.manager.data.SourceInput
import app.reseam.manager.data.bundles
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.parseMarkdown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SourceReviewViewModel(private val graph: AppGraph) : ViewModel() {
    private val imports = graph.sourceImports

    val state: StateFlow<SourceImport?> = imports.current

    /** The source's community listing, found by its link while it's read and by its key once it is. */
    val listing: StateFlow<CommunityBundle?> = combine(imports.current, graph.community.feed) { current, feed ->
        when (current) {
            is SourceImport.Reviewing -> feed.bundles.firstOrNull { it.publicKey == current.staged.metadata.publicKey }
            is SourceImport.Entering -> ((current.checking ?: current.failure?.input) as? SourceInput.Link)?.let { link -> feed.bundles.firstOrNull { it.indexUrl == link.url } }
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val groups: StateFlow<List<PatchGroup>?> = imports.current
        .map { (it as? SourceImport.Reviewing)?.staged?.patches }
        .distinctUntilChanged()
        .mapLatest { patches -> patches?.let { graph.patchGroups(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val filterState = MutableStateFlow<AppFilter>(AppFilter.All)
    val filter: StateFlow<AppFilter> = filterState.asStateFlow()

    /** A README is a nicety: without one the page shows what the bundle itself says. */
    val readme: StateFlow<State?> = listing
        .map { it?.slug }
        .distinctUntilChanged()
        .mapLatest { slug ->
            try {
                slug?.let { graph.community.readme(it) }?.let { withContext(Dispatchers.Default) { parseMarkdown(it) } }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { graph.community.load() }
    }

    fun setFilter(filter: AppFilter) {
        filterState.value = filter
    }

    fun confirm() = imports.confirm()

    fun retry() {
        (state.value as? SourceImport.Entering)?.failure?.input?.let(imports::open)
    }
}
