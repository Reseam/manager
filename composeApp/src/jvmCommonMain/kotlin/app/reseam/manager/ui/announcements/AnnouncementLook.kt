package app.reseam.manager.ui.announcements

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.reseam.manager.data.Announcement
import app.reseam.manager.data.AnnouncementLevel
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.localDate
import app.reseam.manager.ui.theme.extendedColors
import org.jetbrains.compose.resources.stringResource

val AnnouncementLevel.icon: ImageVector
    get() = when (this) {
        AnnouncementLevel.News -> Icons.Megaphone
        AnnouncementLevel.Notice -> Icons.Info
        AnnouncementLevel.Warning -> Icons.Warning
        AnnouncementLevel.Critical -> Icons.CircleAlert
    }

@Composable
fun AnnouncementLevel.tint(): Color = when (this) {
    AnnouncementLevel.News -> MaterialTheme.colorScheme.primary
    AnnouncementLevel.Notice -> MaterialTheme.colorScheme.tertiary
    AnnouncementLevel.Warning -> MaterialTheme.extendedColors.warning
    AnnouncementLevel.Critical -> MaterialTheme.colorScheme.error
}

@Composable
fun AnnouncementLevel.label(): String = stringResource(
    when (this) {
        AnnouncementLevel.News -> Res.string.announcement_news
        AnnouncementLevel.Notice -> Res.string.announcement_notice
        AnnouncementLevel.Warning -> Res.string.announcement_warning
        AnnouncementLevel.Critical -> Res.string.announcement_critical
    },
)

@Composable
fun Announcement.meta(): String = stringResource(Res.string.announcement_meta, level.label(), localDate(createdAt))
