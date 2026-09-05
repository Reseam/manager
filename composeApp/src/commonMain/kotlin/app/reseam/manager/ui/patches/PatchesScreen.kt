package app.reseam.manager.ui.patches

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import app.reseam.manager.sdk.PatchSelection
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.components.Banner
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
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.components.StepIntro
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.components.Toggle
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun PatchesScreen(
    viewModel: PatchesViewModel,
    appName: String,
    onBack: () -> Unit,
    onRun: (target: PatchTarget, selection: PatchSelection, queue: List<String>) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val motion = ReseamTheme.motion
    val ready = state as? PatchesState.Ready
    Screen(
        title = appName,
        onBack = onBack,
        header = { Stepper(current = 1) },
        actions = {
            AnimatedContent(
                targetState = ready?.editor?.enabledCount,
                transitionSpec = { fadeIn(motion.tweenFast()) togetherWith fadeOut(motion.tweenFast()) },
                label = "enabled-count",
            ) { count -> if (count != null) Chip("$count on", variant = ChipVariant.Primary, modifier = Modifier.padding(end = 8.dp)) }
        },
        bottomBar = {
            BottomBar {
                val editor = ready?.editor
                Button(
                    onClick = { ready?.let { onRun(it.target, it.editor.selection(), it.editor.queue()) } },
                    size = ButtonSize.Large,
                    fullWidth = true,
                    enabled = editor != null && editor.enabledCount > 0,
                ) {
                    Text("Patch app")
                    Icon(Icons.Sparkles, null, modifier = Modifier.size(18.dp))
                }
            }
        },
    ) {
        item { StepIntro(step = 2, title = "What to change", body = "Toggle features on or off. Tap a row to tune its options.") }
        when (val current = state) {
            PatchesState.Loading -> item {
                Banner("Inspecting the app and loading compatible patches", variant = app.reseam.manager.ui.components.BannerVariant.Progress, modifier = Modifier.padding(horizontal = 16.dp))
            }
            is PatchesState.Failed -> item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Banner(current.message)
                    Button(onClick = viewModel::inspect, variant = ButtonVariant.Subtle) { Text("Try again") }
                }
            }
            is PatchesState.Ready -> {
                if (current.editor.rows.isEmpty()) {
                    item { EmptyState("No patches for this app", "None of the installed bundles target ${current.target.packageName ?: "this package"}.") }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(viewModel::enableAll, Modifier.weight(1f), ButtonVariant.Ghost, ButtonSize.Small) { Text("Select all") }
                            Button(viewModel::resetDefaults, Modifier.weight(1f), ButtonVariant.Ghost, ButtonSize.Small) { Text("Defaults") }
                        }
                    }
                    items(current.editor.rows, key = { it.id }) { row ->
                        PatchRowCard(
                            row = row,
                            expanded = current.editor.expanded == row.id,
                            onToggle = { viewModel.toggle(row.id, it) },
                            onExpand = { viewModel.expand(if (current.editor.expanded == row.id) null else row.id) },
                            onOption = { key, value -> viewModel.setOption(row.id, key, value) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatchRowCard(
    row: PatchRow,
    expanded: Boolean,
    onToggle: (Boolean) -> Unit,
    onExpand: () -> Unit,
    onOption: (String, app.reseam.manager.sdk.OptionValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val hasOptions = row.meta.options.isNotEmpty()
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp).alpha(if (row.compatible) 1f else 0.55f),
        background = if (row.enabled) colors.surfaceElevated else colors.surfaceSunken,
        borderColor = if (row.enabled) colors.primaryHairline else colors.divider,
        onClick = onExpand,
        enabled = hasOptions && row.enabled,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(row.id, style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
                    Text(row.meta.incompatibility ?: row.meta.description, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
                    if (hasOptions && row.enabled) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val count = row.meta.options.size
                            Text(if (count == 1) "1 option" else "$count options", style = ReseamTheme.typography.captionMedium, color = colors.primary)
                            Icon(Icons.ChevronDown, null, tint = colors.primary, modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = if (expanded) 180f else 0f })
                        }
                    }
                }
                Toggle(checked = row.enabled, onCheckedChange = onToggle, enabled = row.compatible)
            }
            AnimatedVisibility(
                visible = expanded && row.enabled && hasOptions,
                enter = fadeIn(motion.tweenBase()) + expandVertically(motion.tweenBase()),
                exit = fadeOut(motion.tweenFast()) + shrinkVertically(motion.tweenFast()),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Divider()
                    row.meta.options.forEach { declaration ->
                        Box { PatchOptionField(declaration, row.options[declaration.key], onChange = { onOption(declaration.key, it) }) }
                    }
                }
            }
        }
    }
}
