package app.reseam.manager.data

import app.reseam.manager.platform.httpGetText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class AnnouncementLevel { News, Notice, Warning, Critical }

@Serializable
data class Announcement(
    val id: Long,
    val title: String,
    val content: String? = null,
    val author: String? = null,
    @SerialName("level") val levelCode: Int = 0,
    val createdAt: String,
) {
    val level: AnnouncementLevel get() = AnnouncementLevel.entries.getOrElse(levelCode) { AnnouncementLevel.News }
}

sealed interface AnnouncementFeed {
    data object Loading : AnnouncementFeed
    data class Loaded(val announcements: List<Announcement>) : AnnouncementFeed
    data class Failed(val error: Exception) : AnnouncementFeed
}

class AnnouncementRepository(private val settings: SettingsRepository) {
    private val feedState = MutableStateFlow<AnnouncementFeed>(AnnouncementFeed.Loading)
    val feed: StateFlow<AnnouncementFeed> = feedState.asStateFlow()

    suspend fun refresh() {
        feedState.value = try {
            AnnouncementFeed.Loaded(fetchAnnouncements(settings.settings.value.apiBaseUrl).sortedByDescending { it.id })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            (feedState.value as? AnnouncementFeed.Loaded) ?: AnnouncementFeed.Failed(error)
        }
    }

    suspend fun markSeen(id: Long) {
        settings.update { if (id > it.seenAnnouncement) it.copy(seenAnnouncement = id) else it }
    }
}

private suspend fun fetchAnnouncements(apiBaseUrl: String): List<Announcement> =
    ApiJson.decodeFromString(httpGetText(apiBaseUrl.trimEnd('/') + "/announcements"))
