package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.PatchNote
import app.reseam.manager.data.ReleaseNote
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.localDate
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WhatsNewScreen(viewModel: WhatsNewViewModel, onBack: () -> Unit) {
    val whatsNew = viewModel.whatsNew
    val gone = stringResource(Res.string.whats_new_gone)
    SettingsPage(stringResource(Res.string.whats_new_title), onBack) {
        if (whatsNew == null) {
            item { StateMessage(Icons.Layers, gone, null) }
            return@SettingsPage
        }
        item {
            Text(stringResource(Res.string.bundle_updated, viewModel.name.orEmpty(), whatsNew.version), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        }
        whatsNew.notes.filter { it.description.isNotBlank() }.forEach { note -> item(key = note.version) { ReleaseNoteItem(note, Modifier.padding(top = Space.xl)) } }
        patchGroup(Res.string.whats_new_added, whatsNew.added, Icons.Plus)
        patchGroup(Res.string.whats_new_removed, whatsNew.removed, Icons.CircleMinus)
        if (whatsNew.changedOptions > 0) {
            item {
                Text(
                    text = pluralStringResource(Res.plurals.whats_new_options, whatsNew.changedOptions, whatsNew.changedOptions),
                    modifier = Modifier.padding(top = Space.xl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ReleasesScreen(viewModel: ReleasesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val failed = stringResource(Res.string.releases_failed)
    val none = stringResource(Res.string.release_no_notes)
    val retry = DialogAction(stringResource(Res.string.try_again), viewModel::load)
    SettingsPage(stringResource(Res.string.releases_title), onBack) {
        when (val current = state) {
            ReleasesState.Loading -> item { Loading() }
            is ReleasesState.Failed -> item { StateMessage(Icons.Layers, failed, current.error.describe(), action = retry) }
            is ReleasesState.Loaded -> if (current.notes.isEmpty()) {
                item { StateMessage(Icons.Layers, none, null) }
            } else {
                current.notes.forEachIndexed { index, note ->
                    item(key = note.version) { ReleaseNoteItem(note, Modifier.padding(top = if (index == 0) 0.dp else Space.xl)) }
                }
            }
        }
    }
}

private fun LazyListScope.patchGroup(title: StringResource, patches: List<PatchNote>, icon: ImageVector) {
    if (patches.isEmpty()) return
    pageSection(title)
    itemGroup {
        patches.forEach { patch ->
            item { shape -> GroupedItem(patch.name, shape, supporting = patch.description.ifEmpty { null }, icon = icon) }
        }
    }
}

@Composable
private fun ReleaseNoteItem(note: ReleaseNote, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        val heading = if (note.createdAt.isEmpty()) note.version else stringResource(Res.string.release_heading, note.version, localDate(note.createdAt))
        Text(heading, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        SelectionContainer {
            Text(
                text = note.description.ifBlank { stringResource(Res.string.release_no_notes) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
