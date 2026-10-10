package app.reseam.manager.ui.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.Announcement
import app.reseam.manager.data.AnnouncementFeed
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.settings.SettingsPage
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

@Composable
fun AnnouncementsScreen(viewModel: AnnouncementsViewModel, onBack: () -> Unit, onOpen: (Route.Announcement) -> Unit) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val open = LocalDetailRoute.current as? Route.Announcement
    val failed = stringResource(Res.string.announcements_failed)
    val empty = stringResource(Res.string.announcements_empty)
    val retry = DialogAction(stringResource(Res.string.try_again), viewModel::retry)
    SettingsPage(stringResource(Res.string.settings_announcements), onBack) {
        when (val current = feed) {
            AnnouncementFeed.Loading -> item { Loading() }
            is AnnouncementFeed.Failed -> item { StateMessage(Icons.Megaphone, failed, current.error.describe(), action = retry) }
            is AnnouncementFeed.Loaded -> if (current.announcements.isEmpty()) {
                item { StateMessage(Icons.Megaphone, empty, null) }
            } else {
                itemGroup {
                    current.announcements.forEach { announcement ->
                        item(key = announcement.id) { shape ->
                            GroupedItem(
                                title = announcement.title,
                                shape = shape,
                                supporting = announcement.meta(),
                                icon = announcement.level.icon,
                                iconTint = announcement.level.tint(),
                                selected = open?.id == announcement.id,
                                onClick = { onOpen(Route.Announcement(announcement.id)) },
                                trailing = { Chevron() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementScreen(viewModel: AnnouncementViewModel, onBack: () -> Unit) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val failed = stringResource(Res.string.announcements_failed)
    val retry = DialogAction(stringResource(Res.string.try_again), viewModel::retry)
    SettingsPage(stringResource(Res.string.announcement_title), onBack) {
        when (val current = feed) {
            AnnouncementFeed.Loading -> item { Loading() }
            is AnnouncementFeed.Failed -> item { StateMessage(Icons.Megaphone, failed, current.error.describe(), action = retry) }
            is AnnouncementFeed.Loaded -> current.announcements.firstOrNull { it.id == viewModel.id }?.let { announcement(it) }
                ?: item { StateMessage(Icons.Megaphone, failed, null, action = retry) }
        }
    }
}

private fun LazyListScope.announcement(announcement: Announcement) {
    item {
        val tint = announcement.level.tint()
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
            Icon(announcement.level.icon, contentDescription = null, tint = tint, modifier = Modifier.size(Sizes.iconSm))
            Text(announcement.meta(), style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
    item {
        Text(announcement.title, Modifier.padding(top = Space.sm), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
    }
    announcement.content?.takeIf { it.isNotBlank() }?.let { content ->
        item {
            SelectionContainer(Modifier.padding(top = Space.md)) {
                Text(content, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    announcement.author?.let { author ->
        item {
            Text(stringResource(Res.string.announcement_by, author), Modifier.padding(top = Space.lg), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
