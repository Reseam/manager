package app.reseam.manager.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.PatchEditor
import app.reseam.manager.PatchRow
import app.reseam.manager.PatchSource
import app.reseam.manager.resources.*
import app.reseam.manager.sdk.matches
import app.reseam.manager.sdk.option
import app.reseam.manager.sdk.text
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.components.AppHeader
import app.reseam.manager.ui.components.BottomBarLayout
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.IconButtonStyle
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.PatchRow
import app.reseam.manager.ui.components.SearchBar
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.SelectedMark
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.components.TopBar
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.nav.LocalPane
import app.reseam.manager.ui.nav.Pane
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.sdk.PatchPreset
import org.jetbrains.compose.resources.stringResource

@Composable
fun PatchesScreen(session: PatchSession, onDone: () -> Unit) {
    val app by session.app.collectAsStateWithLifecycle()
    val editor by session.editor.collectAsStateWithLifecycle()
    val source by session.source.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var shownSource by rememberSaveable { mutableStateOf<String?>(null) }
    val beside = LocalPane.current == Pane.Detail
    val current = editor
    val sourceFilters = current != null && (current.sources.size > 1 || current.allSourcesOff)
    val shown = current?.sources?.firstOrNull { it.id == shownSource }?.takeIf { sourceFilters }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer).windowInsetsPadding(WindowInsets.statusBars)) {
        TopBar(stringResource(Res.string.patches_title), onBack = onDone.takeUnless { beside }) {
            when {
                current == null || current.allSourcesOff -> Unit
                shown == null -> PresetMenu(current.preset, session::apply)
                shown.used -> PresetMenu(current.preset(shown.name)) { session.apply(it, shown.name) }
            }
        }
        AppHeader(
            app = app,
            supporting = current?.let { patchCounts(it) },
            compact = true,
        )
        BottomBarLayout(
            bar = {
                SearchBar(query, { query = it }, stringResource(Res.string.patches_search), Modifier.weight(1f))
                if (!beside) IconButton(Icons.Check, stringResource(Res.string.patches_done), onDone, style = IconButtonStyle.Filled, size = Sizes.buttonLg)
            },
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(topStart = Radius.md, topEnd = Radius.md)).background(MaterialTheme.colorScheme.surface),
        ) { padding ->
            if (current == null) {
                Loading(Modifier.padding(padding))
            } else {
                LazyColumn(contentPadding = PaddingValues(top = Space.md, bottom = padding.calculateBottomPadding())) {
                    if (sourceFilters) item(key = "sources") { SourceFilters(current, shown) { shownSource = it?.id } }
                    if (shown != null) {
                        item(key = "use") {
                            ItemGroup(Modifier.padding(horizontal = Layout.margin, vertical = Space.sm)) {
                                item { shape ->
                                    SwitchItem(
                                        stringResource(Res.string.patches_source_use, app.name),
                                        shown.used,
                                        { session.use(shown.id, it) },
                                        shape,
                                        supporting = stringResource(Res.string.patches_source_use_body),
                                    )
                                }
                            }
                        }
                    }
                    if (shown?.used != false) patchList(current, shown, query, app.name, source?.versionName, expanded, { expanded = it }, session)
                }
            }
        }
    }
}

/** The patches of [shown], or of every source the app uses, that match [query]. */
private fun LazyListScope.patchList(
    editor: PatchEditor,
    shown: PatchSource?,
    query: String,
    appName: String,
    versionName: String?,
    expanded: String?,
    onExpand: (String?) -> Unit,
    session: PatchSession,
) {
    val failure = editor.failure
    if (failure != null) {
        item(key = "failure") {
            StateMessage(
                Icons.Patch,
                stringResource(Res.string.patches_uncombinable),
                failure.describe(),
                action = DialogAction(stringResource(Res.string.patches_use_recommended), { session.apply(PatchPreset.RECOMMENDED) }),
            )
        }
        return
    }
    val rows = editor.rows.filter { (shown == null || it.reference.bundle == shown.name) && it.text.matches(query) }
    if (rows.isEmpty()) {
        item(key = "empty") {
            if (editor.allSourcesOff) {
                StateMessage(Icons.Patch, stringResource(Res.string.patches_sources_unused), stringResource(Res.string.patches_sources_unused_body))
            } else {
                StateMessage(Icons.Search, stringResource(Res.string.patches_no_match, query.trim()), null)
            }
        }
        return
    }
    val (forApp, forAny) = rows.partition { !it.meta.universal }
    if (forApp.isNotEmpty() && forAny.isNotEmpty()) item { Label(stringResource(Res.string.patches_for_app, appName)) }
    items(forApp, key = { it.reference.text }) { row -> PatchItem(row, editor, versionName, expanded, onExpand, session) }
    if (forAny.isNotEmpty()) item { Label(stringResource(Res.string.patches_for_any)) }
    items(forAny, key = { it.reference.text }) { row -> PatchItem(row, editor, versionName, expanded, onExpand, session) }
}

@Composable
private fun SourceFilters(editor: PatchEditor, shown: PatchSource?, onShow: (PatchSource?) -> Unit) {
    val options = listOf(null to stringResource(Res.string.patches_sources_all)) +
        editor.sources.map { source ->
            // Sources can share a name, so the start of the signer's key tells them apart.
            val name = if (editor.sources.count { it.name == source.name } > 1) stringResource(Res.string.patches_source_key, source.name, source.id.take(4)) else source.name
            source to if (source.used) name else stringResource(Res.string.patches_source_off, name)
        }
    LazyRow(
        Modifier.fillMaxWidth().padding(bottom = Space.sm),
        contentPadding = PaddingValues(horizontal = Layout.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
    ) {
        items(options, key = { (source) -> source?.id.orEmpty() }) { (source, label) ->
            val selected = source?.id == shown?.id
            Chip(label, Modifier.semantics { this.selected = selected }, outlined = !selected, onClick = { onShow(source) })
        }
    }
}

@Composable
private fun Label(text: String) {
    SectionLabel(text, Modifier.padding(horizontal = Layout.margin))
}

@Composable
private fun PatchItem(row: PatchRow, editor: PatchEditor, versionName: String?, expanded: String?, onExpand: (String?) -> Unit, session: PatchSession) {
    val options = row.meta.spec.options
    PatchRow(
        title = row.text.name,
        description = row.text.description,
        checked = row.enabled,
        onCheckedChange = { checked -> session.toggle(row.reference, checked) },
        enabled = editor.selectable(row),
        notes = rowNotes(row, editor, versionName),
        // A patch that runs without the options it needs stays open until they are set.
        expanded = (expanded == row.reference.text || row.enabled && row.neededOptions.isNotEmpty()).takeIf { options.isNotEmpty() },
        onExpand = if (options.isEmpty()) null else ({ onExpand(row.reference.text.takeUnless { it == expanded }) }),
    ) {
        options.forEach { option -> OptionField(option, row.text.option(option.key), row.options[option.key]) { session.setOption(row.reference, option.key, it) } }
    }
}

@Composable
private fun PresetMenu(current: PatchPreset?, onApply: (PatchPreset) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(Icons.More, stringResource(Res.string.more_options), onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
            PatchPreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(presetLabel(preset), style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onApply(preset)
                        open = false
                    },
                    trailingIcon = {
                        if (preset == current) SelectedMark()
                    },
                )
            }
        }
    }
}
