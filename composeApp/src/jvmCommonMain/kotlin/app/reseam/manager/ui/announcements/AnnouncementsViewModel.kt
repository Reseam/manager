package app.reseam.manager.ui.announcements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.AnnouncementFeed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AnnouncementsViewModel(private val graph: AppGraph) : ViewModel() {
    val feed: StateFlow<AnnouncementFeed> = graph.announcements.feed

    init {
        viewModelScope.launch {
            feed.filterIsInstance<AnnouncementFeed.Loaded>().first().announcements.firstOrNull()?.let { graph.announcements.markSeen(it.id) }
        }
    }

    fun retry() {
        viewModelScope.launch { graph.announcements.refresh() }
    }
}

class AnnouncementViewModel(private val graph: AppGraph, val id: Long) : ViewModel() {
    val feed: StateFlow<AnnouncementFeed> = graph.announcements.feed

    init {
        viewModelScope.launch { graph.announcements.markSeen(id) }
    }

    fun retry() {
        viewModelScope.launch { graph.announcements.refresh() }
    }
}
