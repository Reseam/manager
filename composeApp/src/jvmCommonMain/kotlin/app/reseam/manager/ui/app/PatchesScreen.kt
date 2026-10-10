package app.reseam.manager.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.resources.*
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.components.AppHeader
import app.reseam.manager.ui.components.BottomBarLayout
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.IconButtonStyle
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.PatchRow
import app.reseam.manager.ui.components.SearchBar
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.SelectedMark
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.TopBar
import app.reseam.manager.ui.nav.LocalPane
import app.reseam.manager.ui.nav.Pane
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.sdk.PatchPreset
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PatchesScreen(session: PatchSession, onDone: () -> Unit) {
    val app by session.app.collectAsStateWithLifecycle()
    val editor by session.editor.collectAsStateWithLifecycle()
    val source by session.source.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val beside = LocalPane.current == Pane.Detail
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer).windowInsetsPadding(WindowInsets.statusBars)) {
        TopBar(stringResource(Res.string.patches_title), onBack = onDone.takeUnless { beside }) {
            editor?.let { PresetMenu(it.preset, session::apply) }
        }
        val current = editor
        AppHeader(
            app = app,
            supporting = current?.forApp?.let { pluralStringResource(Res.plurals.patches_selected, it.size, it.count { row -> row.enabled }, it.size) }.orEmpty(),
            compact = true,
        )
        BottomBarLayout(
            bar = {
                SearchBar(query, { query = it }, stringResource(Res.string.patches_search), Modifier.weight(1f))
                if (!beside) IconButton(Icons.Check, stringResource(Res.string.patches_done), onDone, style = IconButtonStyle.Filled, size = Sizes.buttonLg)
            },
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(topStart = Radius.md, topEnd = Radius.md)).background(MaterialTheme.colorScheme.surface),
        ) { padding ->
            when {
                current == null -> Loading(Modifier.padding(padding))
                else -> {
                    val needle = query.trim().lowercase()
                    val rows = current.rows.filter { needle.isEmpty() || needle in it.meta.spec.name.lowercase() || needle in it.meta.spec.description.lowercase() }
                    if (rows.isEmpty()) {
                        StateMessage(Icons.Search, stringResource(Res.string.patches_no_match, query.trim()), null, Modifier.padding(padding))
                    } else {
                        val (forApp, forAny) = rows.partition { !it.meta.universal }
                        LazyColumn(contentPadding = PaddingValues(top = Space.md, bottom = padding.calculateBottomPadding())) {
                            if (forApp.isNotEmpty() && forAny.isNotEmpty()) item { Label(stringResource(Res.string.patches_for_app, app.name)) }
                            items(forApp, key = { it.reference }) { row -> PatchItem(row, current, source?.versionName, expanded, { expanded = it }, session) }
                            if (forAny.isNotEmpty()) item { Label(stringResource(Res.string.patches_for_any)) }
                            items(forAny, key = { it.reference }) { row -> PatchItem(row, current, source?.versionName, expanded, { expanded = it }, session) }
                        }
                    }
                }
            }
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
        title = row.meta.spec.name,
        description = row.meta.spec.description,
        checked = row.enabled,
        onCheckedChange = { session.toggle(row.reference, it) },
        enabled = editor.selectable(row),
        note = if (row.compatible) null else versionName?.let { stringResource(Res.string.patches_untested, it) } ?: stringResource(Res.string.patches_untested_newest),
        expanded = (expanded == row.reference).takeIf { options.isNotEmpty() },
        onExpand = if (options.isEmpty()) null else ({ onExpand(row.reference.takeUnless { it == expanded }) }),
    ) {
        options.forEach { option -> OptionField(option, row.options[option.key]) { session.setOption(row.reference, option.key, it) } }
    }
}

@Composable
private fun PresetMenu(current: PatchPreset?, onApply: (PatchPreset) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(Icons.More, stringResource(Res.string.more_options), onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
            listOf(
                PatchPreset.RECOMMENDED to Res.string.preset_recommended,
                PatchPreset.ALL to Res.string.preset_all,
                PatchPreset.NONE to Res.string.preset_none,
            ).forEach { (preset, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label), style = MaterialTheme.typography.bodyLarge) },
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
