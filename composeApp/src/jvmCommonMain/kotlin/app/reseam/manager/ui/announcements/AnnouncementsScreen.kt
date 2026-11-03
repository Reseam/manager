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
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.MarkdownText
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.settings.SettingsPage
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

@Composable
fun AnnouncementScreen(viewModel: AnnouncementViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val failed = stringResource(Res.string.announcements_failed)
    val retry = DialogAction(stringResource(Res.string.try_again), viewModel::retry)
    SettingsPage(stringResource(Res.string.announcement_title), onBack) {
        when (val current = state) {
            AnnouncementState.Loading -> item { Loading() }
            is AnnouncementState.Failed -> item { StateMessage(Icons.Megaphone, failed, current.error?.describe(), action = retry) }
            is AnnouncementState.Loaded -> announcement(current.news)
        }
    }
}

private fun LazyListScope.announcement(news: UpdateItem.News) {
    val announcement = news.announcement
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
    news.body?.let { body ->
        item {
            SelectionContainer(Modifier.padding(top = Space.md)) { MarkdownText(body) }
        }
    }
    announcement.author?.let { author ->
        item {
            Text(stringResource(Res.string.announcement_by, author), Modifier.padding(top = Space.lg), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
