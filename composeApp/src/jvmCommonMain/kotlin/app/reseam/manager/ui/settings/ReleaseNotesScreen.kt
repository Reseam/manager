package app.reseam.manager.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.MarkdownText
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.localDate
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

@Composable
fun WhatsNewScreen(viewModel: WhatsNewViewModel, onBack: () -> Unit) {
    val whatsNew = viewModel.whatsNew
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val gone = stringResource(Res.string.whats_new_gone)
    SettingsPage(stringResource(Res.string.whats_new_title), onBack) {
        if (whatsNew == null) {
            item { StateMessage(Icons.Layers, gone, null) }
            return@SettingsPage
        }
        item {
            Text(stringResource(Res.string.bundle_updated, viewModel.name.orEmpty(), whatsNew.version), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        entries?.let { releaseCards(it, first = false) }
    }
}

@Composable
fun ReleasesScreen(viewModel: ReleasesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val checking by viewModel.checking.collectAsStateWithLifecycle()
    val failed = stringResource(Res.string.releases_failed)
    val none = stringResource(Res.string.release_no_notes)
    val retry = DialogAction(stringResource(Res.string.try_again), viewModel::load)
    SettingsPage(
        title = stringResource(Res.string.releases_title),
        onBack = onBack,
        footer = { Button(stringResource(Res.string.releases_check), viewModel::check, Modifier.weight(1f), style = ButtonStyle.Tonal, icon = Icons.Refresh, loading = checking) },
    ) {
        when (val current = state) {
            ReleasesState.Loading -> item { Loading() }
            is ReleasesState.Failed -> item { StateMessage(Icons.Layers, failed, current.error.describe(), action = retry) }
            is ReleasesState.Loaded -> if (current.entries.isEmpty()) item { StateMessage(Icons.Layers, none, null) } else releaseCards(current.entries, first = true)
        }
    }
}

@Composable
fun ReleaseScreen(viewModel: ReleasesViewModel, title: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val failed = stringResource(Res.string.releases_failed)
    val retry = DialogAction(stringResource(Res.string.try_again), viewModel::load)
    SettingsPage(title, onBack) {
        when (val current = state) {
            ReleasesState.Loading -> item { Loading() }
            is ReleasesState.Failed -> item { StateMessage(Icons.Layers, failed, current.error.describe(), action = retry) }
            is ReleasesState.Loaded -> if (current.entries.isEmpty()) item { StateMessage(Icons.Layers, failed, null, action = retry) } else releaseCards(current.entries, first = true)
        }
    }
}

private fun LazyListScope.releaseCards(entries: List<ReleaseEntry>, first: Boolean) {
    entries.forEachIndexed { index, entry ->
        item(key = entry.note.version) { ReleaseCard(entry, Modifier.padding(top = if (first && index == 0) Space.xs else Space.md)) }
    }
}

@Composable
fun ReleaseCard(entry: ReleaseEntry, modifier: Modifier = Modifier, title: String = entry.note.version, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth().then(if (onClick != null) Modifier.clip(RoundedCornerShape(Radius.lg)).clickable(onClick = onClick) else Modifier), shape = RoundedCornerShape(Radius.lg), color = colors.surfaceContainer) {
        Column(Modifier.padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = colors.primary)
                if (entry.note.createdAt.isNotEmpty()) Text(localDate(entry.note.createdAt), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            val body = entry.body
            if (body == null) {
                Text(stringResource(Res.string.release_no_notes), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            } else {
                SelectionContainer { MarkdownText(body) }
            }
        }
    }
}
