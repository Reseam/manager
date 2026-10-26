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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UpdateItem {
    val createdAt: String

    data class News(val announcement: Announcement) : UpdateItem {
        override val createdAt: String get() = announcement.createdAt
    }

    /** A release of the official patches ([source] names them) or, without a source, of Reseam Manager. */
    data class Release(val source: Bundle?, val entry: ReleaseEntry, val prerelease: Boolean) : UpdateItem {
        override val createdAt: String get() = entry.note.createdAt
    }
}

sealed interface UpdatesState {
    data object Loading : UpdatesState
    data class Loaded(val items: List<UpdateItem>) : UpdatesState
    data class Failed(val error: Exception) : UpdatesState
}

class UpdatesViewModel(private val graph: AppGraph) : ViewModel() {
    private val releases = MutableStateFlow<Result<List<UpdateItem.Release>>?>(null)

    val state: StateFlow<UpdatesState> = combine(graph.announcements.feed, releases) { feed, releases ->
        val news = (feed as? AnnouncementFeed.Loaded)?.announcements?.map { UpdateItem.News(it) }
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
    val feed: StateFlow<AnnouncementFeed> = graph.announcements.feed

    init {
        viewModelScope.launch { graph.announcements.markSeen(id) }
    }

    fun retry() {
        viewModelScope.launch { graph.announcements.refresh() }
    }
}
