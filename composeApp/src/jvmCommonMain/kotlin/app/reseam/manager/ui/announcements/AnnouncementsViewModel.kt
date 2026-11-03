package app.reseam.manager.ui.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Announcement
import app.reseam.manager.data.AnnouncementFeed
import app.reseam.manager.data.Bundle
import app.reseam.manager.data.ReleaseInfo
import app.reseam.manager.data.fetchBundleIndex
import app.reseam.manager.data.fetchManagerReleases
import app.reseam.manager.data.note
import app.reseam.manager.data.officialIndexUrl
import app.reseam.manager.ui.settings.ReleaseEntry
import app.reseam.manager.ui.settings.entries
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.parseMarkdown
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UpdateItem {
    val createdAt: String

    /** [body] is the announcement's markdown, parsed ahead of composition as release notes are. */
    data class News(val announcement: Announcement, val body: State?) : UpdateItem {
        override val createdAt: String get() = announcement.createdAt
    }

    /** A release of the official patches ([source] names them) or, without a source, of Reseam Manager. */
    data class Release(val source: Bundle?, val entry: ReleaseEntry, val prerelease: Boolean) : UpdateItem {
        override val createdAt: String get() = entry.note.createdAt
    }
}

private suspend fun Announcement.news(): UpdateItem.News =
    UpdateItem.News(this, content?.takeIf { it.isNotBlank() }?.let { withContext(Dispatchers.Default) { parseMarkdown(it) } })

sealed interface AnnouncementState {
    data object Loading : AnnouncementState

    /** Without an [error], the feed loaded but no longer has the announcement. */
    data class Failed(val error: Exception?) : AnnouncementState

    data class Loaded(val news: UpdateItem.News) : AnnouncementState
}

sealed interface UpdatesState {
    data object Loading : UpdatesState
    data class Loaded(val items: List<UpdateItem>) : UpdatesState
    data class Failed(val error: Exception) : UpdatesState
}

class UpdatesViewModel(private val graph: AppGraph) : ViewModel() {
    private val releases = MutableStateFlow<Result<List<UpdateItem.Release>>?>(null)

    val state: StateFlow<UpdatesState> = combine(graph.announcements.feed, releases) { feed, releases ->
        val news = (feed as? AnnouncementFeed.Loaded)?.announcements?.map { it.news() }
        val loaded = releases?.getOrNull()
        when {
            feed is AnnouncementFeed.Loading || releases == null -> UpdatesState.Loading
            news == null && loaded == null -> UpdatesState.Failed((feed as AnnouncementFeed.Failed).error)
            else -> UpdatesState.Loaded((news.orEmpty() + loaded.orEmpty()).sortedByDescending { it.createdAt })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpdatesState.Loading)

    init {
        viewModelScope.launch {
            graph.announcements.feed.filterIsInstance<AnnouncementFeed.Loaded>().first().announcements.firstOrNull()?.let { graph.announcements.markSeen(it.id) }
        }
        loadReleases()
    }

    fun retry() {
        viewModelScope.launch { graph.announcements.refresh() }
        loadReleases()
    }

    private fun loadReleases() {
        viewModelScope.launch {
            val settings = graph.settings.settings.value
            val official = graph.bundles.installed().firstOrNull { it.official }
            releases.value = try {
                val patches = official?.let { bundle ->
                    fetchBundleIndex(officialIndexUrl(settings.serverUrl)).releases.filter { bundle.prereleases || !it.prerelease }.items(bundle)
                }.orEmpty()
                val manager = fetchManagerReleases(settings.serverUrl).filter { settings.managerPrereleases || !it.prerelease }.items(null)
                Result.success(patches + manager)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }
}

private suspend fun List<ReleaseInfo>.items(source: Bundle?): List<UpdateItem.Release> =
    map { it.note() }.entries().zip(this) { entry, info -> UpdateItem.Release(source, entry, info.prerelease) }

class AnnouncementViewModel(private val graph: AppGraph, val id: Long) : ViewModel() {
    val state: StateFlow<AnnouncementState> = graph.announcements.feed.map { feed ->
        when (feed) {
            AnnouncementFeed.Loading -> AnnouncementState.Loading
            is AnnouncementFeed.Failed -> AnnouncementState.Failed(feed.error)
            is AnnouncementFeed.Loaded -> feed.announcements.firstOrNull { it.id == id }?.let { AnnouncementState.Loaded(it.news()) } ?: AnnouncementState.Failed(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnnouncementState.Loading)

    init {
        viewModelScope.launch { graph.announcements.markSeen(id) }
    }

    fun retry() {
        viewModelScope.launch { graph.announcements.refresh() }
    }
}
