package app.reseam.manager.ui.patches

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.sdk.OptionValue
import app.reseam.manager.sdk.PatchMetadata
import app.reseam.manager.sdk.PatchSelection
import app.reseam.manager.sdk.Problem
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BannerVariant
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.DetailPlaceholder
import app.reseam.manager.ui.components.Divider
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ScreenFrame
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.StepInstruction
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.components.Toggle
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme
import app.reseam.manager.userMessage

@Composable
fun PatchesScreen(
    viewModel: PatchesViewModel,
    appName: String,
    onBack: () -> Unit,
    onRun: (target: PatchTarget, selection: PatchSelection, queue: List<String>, bundlePaths: List<String>, patches: List<PatchMetadata>) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout = ReseamTheme.layout
    val ready = state as? PatchesState.Ready
    ScreenFrame(
        title = appName,
        onBack = onBack,
        header = { Stepper(current = 1) },
        wide = true,
        bottomBar = {
            BottomBar { fill ->
                val editor = ready?.editor
                Button(
                    onClick = { ready?.let { onRun(it.target, it.editor.selection(), it.editor.queue(), it.bundlePaths, it.response.patches) } },
                    modifier = fill,
                    size = ButtonSize.Large,
                    enabled = editor != null && editor.enabledCount > 0,
                    icon = Icons.Sparkles,
                ) { Text("Patch app") }
            }
        },
    ) {
        val current = state
        if (current is PatchesState.Ready && layout.twoPane && current.editor.rows.isNotEmpty()) {
            SplitBody(current, viewModel)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = layout.pageMargin, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (current) {
                    PatchesState.Loading -> item { Banner("Inspecting the app and loading compatible patches", variant = BannerVariant.Progress) }
                    is PatchesState.Failed -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Banner(current.message)
                            Button(onClick = viewModel::inspect, variant = ButtonVariant.Subtle, icon = Icons.Refresh) { Text("Try again") }
                        }
                    }
                    is PatchesState.Ready -> if (current.editor.rows.isEmpty()) {
                        problemItems(current, viewModel)
                        item {
                            if (current.problems.isEmpty()) {
                                EmptyState("No patches for this app", "None of the installed bundles target ${current.target.packageName ?: "this package"}.", icon = Icons.Puzzle)
                            } else {
                                EmptyState("No patches available", "Sort out the bundle above to see its patches.", icon = Icons.Puzzle)
                            }
                        }
                    } else {
                        patchList(current, viewModel, inline = true)
                    }
                }
            }
        }
    }
}

/** The list on the left, the selected patch on the right. */
@Composable
private fun SplitBody(state: PatchesState.Ready, viewModel: PatchesViewModel) {
    val layout = ReseamTheme.layout
    val colors = ReseamTheme.colors
    val selected = state.editor.rows.firstOrNull { it.id == state.editor.selected }
    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(start = layout.pageMargin, end = layout.gutter, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            patchList(state, viewModel, inline = false)
        }
        Column(
            modifier = Modifier.width(1.dp).fillMaxHeight().padding(vertical = 16.dp),
        ) { Divider(Modifier.fillMaxHeight().width(1.dp)) }
        Column(
            modifier = Modifier.width(420.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = layout.gutter, end = layout.pageMargin, top = 16.dp, bottom = 24.dp),
        ) {
            if (selected == null) {
                DetailPlaceholder(Icons.Sparkles, "Patch details", "Select a patch to read what it does and set its options.")
            } else {
                PatchDetail(
                    row = selected,
                    editor = state.editor,
                    onToggle = { viewModel.toggle(selected.id, it) },
                    onOption = { key, value -> viewModel.setOption(selected.id, key, value) },
                )
            }
        }
    }
    if (state.editor.rows.isEmpty()) Text("", color = colors.foreground)
}

private fun LazyListScope.patchList(state: PatchesState.Ready, viewModel: PatchesViewModel, inline: Boolean) {
    val editor = state.editor
    item { StepInstruction("Choose what to change") }
    problemItems(state, viewModel)
    item { CompatibilityNotice(editor, state.target, onAllow = viewModel::allowIncompatible) }
    item {
        // The recommended set is where most people should be, so it reads as the fuller
        // of the two; "Defaults" named the mechanism rather than what you get.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(viewModel::resetDefaults, Modifier.weight(1f), ButtonVariant.Subtle, ButtonSize.Small) { Text("Recommended") }
            Button(viewModel::enableAll, Modifier.weight(1f), ButtonVariant.Ghost, ButtonSize.Small) { Text("Select all") }
        }
    }
    val (universal, specific) = editor.rows.partition { it.universal }
    if (specific.isNotEmpty() && universal.isNotEmpty()) item { SectionHeader("For ${state.target.name}") }
    patchRows(specific, editor, viewModel, inline)
    if (specific.isNotEmpty() && universal.isNotEmpty()) item { SectionHeader("Works with any app") }
    patchRows(universal, editor, viewModel, inline)
}

private fun LazyListScope.patchRows(rows: List<PatchRow>, editor: PatchEditor, viewModel: PatchesViewModel, inline: Boolean) {
    items(rows, key = { it.id }) { row ->
        PatchRowCard(
            row = row,
            editor = editor,
            selected = editor.selected == row.id,
            inline = inline,
            onToggle = { viewModel.toggle(row.id, it) },
            onSelect = { viewModel.select(if (inline && editor.selected == row.id) null else row.id) },
            onOption = { key, value -> viewModel.setOption(row.id, key, value) },
            modifier = Modifier.animateItem(),
        )
    }
}

private fun LazyListScope.problemItems(state: PatchesState.Ready, viewModel: PatchesViewModel) {
    items(state.problems, key = { "problem:" + it.fileName }) { problem -> BundleProblemBanner(problem, viewModel) }
}

@Composable
private fun BundleProblemBanner(problem: BundleProblem, viewModel: PatchesViewModel) {
    val name = problem.bundle?.name ?: problem.fileName
    val bundle = problem.bundle
    val action: Pair<String, () -> Unit>? = when {
        problem.problem is Problem.BundleTooOld && bundle?.official == true -> "Update" to viewModel::updateOfficialBundle
        problem.problem is Problem.UnreadableBundle && bundle != null -> "Remove" to { viewModel.removeBundle(bundle.id) }
        else -> null
    }
    Banner(
        message = problem.problem.userMessage(name) ?: "$name can't be used right now.",
        trailing = { action?.let { (label, run) -> Button(onClick = run, variant = ButtonVariant.Ghost, size = ButtonSize.Small) { Text(label) } } },
    )
}

@Composable
private fun CompatibilityNotice(editor: PatchEditor, target: PatchTarget, onAllow: () -> Unit) {
    val count = editor.incompatibleCount
    if (count == 0) return
    val version = target.versionName?.let { "version $it" } ?: "this version"
    val patches = if (count == 1) "1 patch is" else "$count patches are"
    if (!editor.allowIncompatible) {
        Banner(
            message = "$patches made for other versions of ${target.name}, not $version.",
            variant = BannerVariant.Neutral,
            trailing = { Button(onClick = onAllow, variant = ButtonVariant.Ghost, size = ButtonSize.Small) { Text("Allow") } },
        )
    } else {
        Banner("$patches not made for $version. Enable them at your own risk: they may fail or break the app.")
    }
}

/** Header of a patch card: name, description, and the reasons it needs a second look. */
@Composable
private fun PatchSummary(row: PatchRow, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(row.meta.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
        if (row.meta.description.isNotBlank()) Text(row.meta.description, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
        row.meta.incompatibility?.let { Text(it, style = ReseamTheme.typography.captionSmall, color = colors.warningForeground) }
    }
}

/** Names the enabled patches that keep [row] on, so a toggle the user did not set looks deliberate. */
@Composable
private fun RequiredByChip(row: PatchRow, editor: PatchEditor) {
    if (!row.enabled) return
    val required = editor.requiredBy(row.id)
    val first = required.firstOrNull() ?: return
    val label = if (required.size == 1) {
        "Required by ${first.meta.name}"
    } else {
        "Required by ${first.meta.name} and ${required.size - 1} more"
    }
    Chip(label, variant = ChipVariant.Primary)
}

@Composable
private fun PatchRowCard(
    row: PatchRow,
    editor: PatchEditor,
    selected: Boolean,
    inline: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onOption: (String, OptionValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val hasOptions = row.meta.options.isNotEmpty()
    val selectable = editor.selectable(row)
    val expandable = inline && hasOptions && row.enabled
    Card(
        modifier = modifier.fillMaxWidth().alpha(if (selectable) 1f else 0.6f),
        background = when {
            selected && !inline -> colors.primaryFaint
            row.enabled -> colors.surfaceElevated
            else -> colors.surfaceSunken
        },
        borderColor = when {
            selected && !inline -> colors.primaryHairline
            row.enabled -> colors.primaryHairline
            else -> colors.divider
        },
        onClick = onSelect,
        enabled = !inline || expandable,
        contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PatchSummary(row)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!row.compatible) Chip("Untested version", variant = ChipVariant.Warning)
                        RequiredByChip(row, editor)
                        if (expandable) {
                            val count = row.meta.options.size
                            Text(if (count == 1) "1 option" else "$count options", style = ReseamTheme.typography.captionMedium, color = colors.primary)
                            Icon(Icons.ChevronDown, null, tint = colors.primary, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = if (selected) 180f else 0f })
                        } else if (hasOptions && !inline) {
                            Chip(if (row.meta.options.size == 1) "1 option" else "${row.meta.options.size} options")
                        }
                    }
                }
                Toggle(checked = row.enabled, onCheckedChange = onToggle, enabled = selectable)
            }
            AnimatedVisibility(
                visible = inline && selected && expandable,
                enter = fadeIn(motion.tweenBase()) + expandVertically(motion.tweenBase()),
                exit = fadeOut(motion.tweenFast()) + shrinkVertically(motion.tweenFast()),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Divider()
                    PatchOptions(row, onOption)
                }
            }
        }
    }
}

/** The selected patch in full, for the pane beside the list. */
@Composable
private fun PatchDetail(row: PatchRow, editor: PatchEditor, onToggle: (Boolean) -> Unit, onOption: (String, OptionValue) -> Unit) {
    val colors = ReseamTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PatchSummary(row)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(row.meta.bundle)
            if (row.meta.enabledByDefault) Chip("Default", variant = ChipVariant.Primary)
            if (!row.compatible) Chip("Untested version", variant = ChipVariant.Warning)
            RequiredByChip(row, editor)
        }
        Card(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (row.enabled) "Enabled" else "Disabled", style = ReseamTheme.typography.bodyMedium, color = colors.foreground, modifier = Modifier.weight(1f))
                Toggle(checked = row.enabled, onCheckedChange = onToggle, enabled = editor.selectable(row))
            }
        }
        if (row.meta.options.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionLabel("Options", modifier = Modifier.padding(start = 4.dp))
                if (row.enabled) PatchOptions(row, onOption)
                else Text("Enable the patch to set its options.", style = ReseamTheme.typography.caption, color = colors.mutedForeground)
            }
        }
        val versions = row.meta.declared.flatMap { it.versions }
        if (versions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("Made for", modifier = Modifier.padding(start = 4.dp))
                Text(versions.joinToString(", "), style = ReseamTheme.typography.monoSmall, color = colors.mutedForeground)
            }
        }
    }
}

@Composable
private fun PatchOptions(row: PatchRow, onOption: (String, OptionValue) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        row.meta.options.forEach { declaration ->
            PatchOptionField(declaration, row.options[declaration.key], onChange = { onOption(declaration.key, it) })
        }
    }
}
