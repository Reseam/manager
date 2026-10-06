package app.reseam.manager.ui.patches

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.sdk.description
import app.reseam.manager.sdk.name
import app.reseam.manager.sdk.options
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BannerVariant
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.Divider
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.PatchFlowChrome
import app.reseam.manager.ui.components.ItemTextSpacing
import app.reseam.manager.ui.components.ScreenFrame
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.SectionSpacing
import app.reseam.manager.ui.components.Segment
import app.reseam.manager.ui.components.SegmentedControl
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.SheetHeader
import app.reseam.manager.ui.components.StepInstruction
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.components.Toggle
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme
import app.reseam.manager.userMessage
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.OptionValue
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.PatchPreset
import app.reseam.sdk.PatchSelection
import app.reseam.sdk.Problem

private val PresetSegments = listOf(
    Segment(PatchPreset.ALL, "All"),
    Segment(PatchPreset.RECOMMENDED, "Recommended"),
    Segment(PatchPreset.NONE, "None"),
)

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
        chromeKey = PatchFlowChrome,
        maxContentWidth = if (layout.twoPane) Dp.Infinity else null,
        bottomBar = {
            BottomBar(wide = true, maxContentWidth = if (layout.twoPane) Dp.Infinity else null) { fill ->
                if (!layout.compact && ready != null) {
                    Text("${ready.editor.enabledCount} patches selected", style = ReseamTheme.typography.bodySmall, color = ReseamTheme.colors.mutedForeground, modifier = Modifier.weight(1f))
                }
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
            PatchGrid(current, viewModel)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = layout.pageMargin, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(SectionSpacing),
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
                        patchList(current, viewModel)
                    }
                }
            }
        }
    }
}

/** More patches share the desktop width; their options expand in the same card. */
@Composable
private fun PatchGrid(state: PatchesState.Ready, viewModel: PatchesViewModel) {
    val layout = ReseamTheme.layout
    var query by rememberSaveable { mutableStateOf("") }
    val selected = state.editor.rows.firstOrNull { it.reference == state.editor.selected }
    val rows = state.editor.rows.filter { it.meta.name.contains(query, ignoreCase = true) || it.meta.description.contains(query, ignoreCase = true) }
    val (universal, specific) = rows.partition { it.universal }
    Column(Modifier.fillMaxSize().padding(horizontal = layout.pageMargin, vertical = 8.dp)) {
        StepInstruction("Choose what to change")
        Row(
            modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(layout.gutter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SegmentedControl(PresetSegments, state.editor.preset, viewModel::apply, modifier = Modifier.weight(1f))
            TextField(query, { query = it }, placeholder = "Search patches", leading = Icons.Search, modifier = Modifier.weight(1f))
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(layout.gutter),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        ) {
            if (state.target.installMethod == InstallMethod.MOUNT) item(span = { GridItemSpan(maxLineSpan) }) { MountNotice(state.target.name) }
            items(state.problems, key = { "problem:" + it.fileName }, span = { GridItemSpan(maxLineSpan) }) { problem ->
                BundleProblemBanner(problem, viewModel)
            }
            if (state.editor.incompatibleCount > 0) item(span = { GridItemSpan(maxLineSpan) }) {
                CompatibilityNotice(state.editor, state.target, onAllow = viewModel::allowIncompatible)
            }
            if (rows.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState("No matching patches", "Try a different search.", icon = Icons.Search)
            }
            if (specific.isNotEmpty() && universal.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("For ${state.target.name}")
            }
            patchRows(specific, state.editor, viewModel)
            if (specific.isNotEmpty() && universal.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("Works with any app")
            }
            patchRows(universal, state.editor, viewModel)
        }
    }
    selected?.let { row ->
        Sheet(onDismiss = { viewModel.select(null) }) {
            Column(Modifier.heightIn(max = 600.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SheetHeader(row.meta.name, row.meta.description)
                row.meta.incompatibility?.let { Banner(it) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", style = ReseamTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Toggle(row.enabled, { viewModel.toggle(row.reference, it) }, enabled = state.editor.selectable(row))
                }
                RequiredByChip(row, state.editor)
                if (row.meta.options.isNotEmpty()) {
                    if (row.enabled) PatchOptions(row, onOption = { key, value -> viewModel.setOption(row.reference, key, value) })
                    else Text("Enable the patch to set its options.", style = ReseamTheme.typography.caption, color = ReseamTheme.colors.mutedForeground)
                }
            }
        }
    }
}

private fun LazyGridScope.patchRows(rows: List<PatchRow>, editor: PatchEditor, viewModel: PatchesViewModel) {
    items(rows, key = { it.reference }) { row ->
        DesktopPatchCard(
            row = row,
            editor = editor,
            onToggle = { viewModel.toggle(row.reference, it) },
            onSelect = { viewModel.select(row.reference) },
            modifier = Modifier.animateItem(),
        )
    }
}

private fun LazyListScope.patchList(state: PatchesState.Ready, viewModel: PatchesViewModel) {
    val editor = state.editor
    item { StepInstruction("Choose what to change") }
    if (state.target.installMethod == InstallMethod.MOUNT) item { MountNotice(state.target.name) }
    problemItems(state, viewModel)
    item { CompatibilityNotice(editor, state.target, onAllow = viewModel::allowIncompatible) }
    item { SegmentedControl(PresetSegments, editor.preset, viewModel::apply) }
    val (universal, specific) = editor.rows.partition { it.universal }
    if (specific.isNotEmpty() && universal.isNotEmpty()) item { SectionHeader("For ${state.target.name}") }
    patchRows(specific, editor, viewModel)
    if (specific.isNotEmpty() && universal.isNotEmpty()) item { SectionHeader("Works with any app") }
    patchRows(universal, editor, viewModel)
}

private fun LazyListScope.patchRows(rows: List<PatchRow>, editor: PatchEditor, viewModel: PatchesViewModel) {
    // No animateItem(): the in-card options expand/collapse already resizes the item, and a separate
    // placement spring outlasts the shrink, leaving a gap before the rows below catch up. The list
    // keeps a stable order, so there is nothing else for a placement animation to do.
    items(rows, key = { it.reference }) { row ->
        PatchRowCard(
            row = row,
            editor = editor,
            selected = editor.selected == row.reference,
            onToggle = { viewModel.toggle(row.reference, it) },
            onSelect = { viewModel.select(if (editor.selected == row.reference) null else row.reference) },
            onOption = { key, value -> viewModel.setOption(row.reference, key, value) },
        )
    }
}

@Composable
private fun MountNotice(appName: String) {
    Banner("Mounting over the installed $appName. Patches that only work as a separate app are left out.", variant = BannerVariant.Neutral)
}

private fun LazyListScope.problemItems(state: PatchesState.Ready, viewModel: PatchesViewModel) {
    items(state.problems, key = { "problem:" + it.fileName }) { problem -> BundleProblemBanner(problem, viewModel) }
}

@Composable
private fun BundleProblemBanner(problem: BundleProblem, viewModel: PatchesViewModel) {
    val name = problem.bundle?.name ?: problem.fileName
    val bundle = problem.bundle
    val action: Pair<String, () -> Unit>? = when {
        problem.problem is Problem.BundleTooOld && bundle?.followsUpdates == true -> "Update" to viewModel::updateBundles
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(ItemTextSpacing)) {
        Text(row.meta.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
        if (row.meta.description.isNotBlank()) Text(row.meta.description, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
        row.meta.incompatibility?.let { Text(it, style = ReseamTheme.typography.captionSmall, color = colors.warningForeground) }
    }
}

/** Names the enabled patches that keep [row] on, so a toggle the user did not set looks deliberate. */
@Composable
private fun RequiredByChip(row: PatchRow, editor: PatchEditor) {
    if (!row.enabled) return
    val required = editor.requiredBy(row.reference)
    val first = required.firstOrNull() ?: return
    val label = if (required.size == 1) {
        "Required by ${first.meta.name}"
    } else {
        "Required by ${first.meta.name} and ${required.size - 1} more"
    }
    Chip(label, variant = ChipVariant.Primary)
}

/** Every grid card reserves one title line, two description lines, and one metadata row. */
@Composable
private fun DesktopPatchCard(row: PatchRow, editor: PatchEditor, onToggle: (Boolean) -> Unit, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    val typography = ReseamTheme.typography
    val height = with(LocalDensity.current) {
        typography.bodyMedium.lineHeight.toDp() + typography.caption.lineHeight.toDp() * 2 + typography.captionMedium.lineHeight.toDp()
    } + 40.dp
    val selectable = editor.selectable(row)
    Card(
        modifier = modifier.fillMaxWidth().height(height).alpha(if (selectable) 1f else 0.6f),
        background = colors.surface,
        borderColor = if (row.enabled) colors.primaryHairline else colors.divider,
        onClick = onSelect,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(row.meta.name, style = typography.bodyMedium, color = colors.foreground, minLines = 1, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Toggle(row.enabled, onToggle, enabled = selectable, small = true)
            }
            Text(row.meta.description, style = typography.caption, color = colors.mutedForeground, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (!row.compatible) "Untested version" else if (editor.requiredBy(row.reference).isNotEmpty() && row.enabled) "Required by other patches" else "",
                    style = typography.captionSmall,
                    color = if (!row.compatible) colors.warningForeground else colors.mutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (row.meta.options.isNotEmpty()) {
                    val count = row.meta.options.size
                    Text(if (count == 1) "1 option" else "$count options", style = typography.captionMedium, color = colors.primary)
                    Icon(Icons.ChevronDown, null, tint = colors.primary, modifier = Modifier.padding(start = 6.dp).size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PatchRowCard(
    row: PatchRow,
    editor: PatchEditor,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onOption: (String, OptionValue?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val hasOptions = row.meta.options.isNotEmpty()
    val selectable = editor.selectable(row)
    val expandable = hasOptions && row.enabled
    Card(
        modifier = modifier.fillMaxWidth().alpha(if (selectable) 1f else 0.6f),
        // On is already said by the toggle and the mint edge; a third fill made the list
        // a wall of slabs with nothing standing out.
        background = colors.surface,
        borderColor = if (row.enabled) colors.primaryHairline else colors.divider,
        onClick = onSelect,
        enabled = expandable,
        contentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PatchSummary(row)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!row.compatible) Chip("Untested version", variant = ChipVariant.Warning)
                        RequiredByChip(row, editor)
                        if (expandable) {
                            val count = row.meta.options.size
                            Text(if (count == 1) "1 option" else "$count options", style = ReseamTheme.typography.captionMedium, color = colors.primary)
                            Icon(Icons.ChevronDown, null, tint = colors.primary, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = if (selected) 180f else 0f })
                        }
                    }
                }
                Toggle(checked = row.enabled, onCheckedChange = onToggle, enabled = selectable)
            }
            AnimatedVisibility(
                visible = selected && expandable,
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

@Composable
private fun PatchOptions(row: PatchRow, onOption: (String, OptionValue?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        row.meta.options.forEach { declaration ->
            PatchOptionField(declaration, row.options[declaration.key], onChange = { onOption(declaration.key, it) })
        }
    }
}
