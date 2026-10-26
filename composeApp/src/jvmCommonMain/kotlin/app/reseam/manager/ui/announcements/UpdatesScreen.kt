package app.reseam.manager.ui.announcements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.Announcement
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.localDate
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.settings.ReleaseCard
import app.reseam.manager.ui.settings.SettingsPage
import app.reseam.manager.ui.settings.pageSection
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

private const val Expanded = 5

@Composable
fun UpdatesScreen(viewModel: UpdatesViewModel, onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val open = LocalDetailRoute.current as? Route
    val failed = stringResource(Res.string.updates_failed)
    val empty = stringResource(Res.string.updates_empty)
    val retry = DialogAction(stringResource(Res.string.try_again), viewModel::retry)
    SettingsPage(stringResource(Res.string.settings_updates), onBack) {
        when (val current = state) {
            UpdatesState.Loading -> item { Loading() }
            is UpdatesState.Failed -> item { StateMessage(Icons.Megaphone, failed, current.error.describe(), action = retry) }
            is UpdatesState.Loaded -> if (current.items.isEmpty()) {
                item { StateMessage(Icons.Megaphone, empty, null) }
            } else {
                current.items.take(Expanded).forEachIndexed { index, update ->
                    item(key = update.key) { ExpandedUpdate(update, onOpen, Modifier.padding(top = if (index == 0) Space.xs else Space.md)) }
                }
                val earlier = current.items.drop(Expanded)
                if (earlier.isNotEmpty()) {
                    pageSection(Res.string.updates_earlier)
                    itemGroup {
                        earlier.forEach { update ->
                            item(key = update.key) { shape -> UpdateRow(update, shape, open, onOpen) }
                        }
                    }
                }
            }
        }
    }
}

private val UpdateItem.key: String
    get() = when (this) {
        is UpdateItem.News -> "news-${announcement.id}"
        is UpdateItem.Release -> "${source?.id ?: "manager"}-${entry.note.version}"
    }

private val UpdateItem.route: Route
    get() = when (this) {
        is UpdateItem.News -> Route.Announcement(announcement.id)
        is UpdateItem.Release -> Route.Release(source?.id, entry.note.version)
    }

@Composable
private fun UpdateItem.Release.title(): String =
    stringResource(Res.string.update_release_title, stringResource(if (source == null) Res.string.app_name else Res.string.updates_patches), entry.note.version)

@Composable
private fun ExpandedUpdate(update: UpdateItem, onOpen: (Route) -> Unit, modifier: Modifier) {
    when (update) {
        is UpdateItem.Release -> ReleaseCard(update.entry, modifier, title = update.title(), onClick = { onOpen(update.route) })
        is UpdateItem.News -> {
            val colors = MaterialTheme.colorScheme
            val announcement = update.announcement
            Surface(
                modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.lg)).clickable { onOpen(update.route) },
                shape = RoundedCornerShape(Radius.lg),
                color = colors.surfaceContainer,
            ) {
                Column(Modifier.padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                        Icon(announcement.level.icon, contentDescription = null, tint = announcement.level.tint(), modifier = Modifier.size(Sizes.iconSm))
                        Text(announcement.meta(), style = MaterialTheme.typography.labelLarge, color = announcement.level.tint())
                    }
                    Text(announcement.title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    announcement.content?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateRow(update: UpdateItem, shape: Shape, open: Route?, onOpen: (Route) -> Unit) {
    when (update) {
        is UpdateItem.News -> GroupedItem(
            title = update.announcement.title,
            shape = shape,
            supporting = update.announcement.meta(),
            icon = update.announcement.level.icon,
            iconTint = update.announcement.level.tint(),
            selected = open == update.route,
            onClick = { onOpen(update.route) },
            trailing = { Chevron() },
        )
        is UpdateItem.Release -> {
            val kind = stringResource(if (update.prerelease) Res.string.update_prerelease else Res.string.update_release)
            GroupedItem(
                title = update.title(),
                shape = shape,
                supporting = stringResource(Res.string.announcement_meta, kind, localDate(update.createdAt)),
                icon = if (update.source == null) Icons.Phone else Icons.Layers,
                selected = open == update.route,
                onClick = { onOpen(update.route) },
                trailing = { Chevron() },
            )
        }
    }
}
