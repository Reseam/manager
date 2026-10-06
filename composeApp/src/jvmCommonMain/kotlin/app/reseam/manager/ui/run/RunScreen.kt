package app.reseam.manager.ui.run

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.PatchStatus
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BannerVariant
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.LogDrawer
import app.reseam.manager.ui.components.LogPane
import app.reseam.manager.ui.components.PatchFlowChrome
import app.reseam.manager.ui.components.ProgressBar
import app.reseam.manager.ui.components.ScreenFrame
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.SectionSpacing
import app.reseam.manager.ui.components.PatchFlowSteps
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.components.Segment
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.components.TabBar
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme

private enum class ReportTab { Patches, Log }

private val ReportSegments = listOf(Segment(ReportTab.Patches, "Patches"), Segment(ReportTab.Log, "Log"))
private val QueueCellMinWidth = 300.dp
private val DesktopAppIconSize = 48.dp

@Composable
fun RunScreen(
    viewModel: RunViewModel,
    target: PatchTarget,
    queue: List<String>,
    artifactActionLabel: String,
    onDone: () -> Unit,
    onInstallInstead: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val layout = ReseamTheme.layout
    val finished = state.phase != RunPhase.Running
    ScreenFrame(
        title = when (state.phase) {
            RunPhase.Running -> "Patching"
            RunPhase.Finished -> if (state.failed.isEmpty()) "Ready" else "Ready, with warnings"
            RunPhase.Failed -> "Patching failed"
        },
        onBack = if (finished) onDone else null,
        header = { Stepper(current = if (state.phase == RunPhase.Finished) PatchFlowSteps.size else 2) },
        wide = true,
        chromeKey = PatchFlowChrome,
        maxContentWidth = if (layout.twoPane) Dp.Infinity else null,
        actions = {
            if (state.phase == RunPhase.Finished && !layout.twoPane) {
                Button(onClick = viewModel::saveArtifact, size = ButtonSize.Small, variant = ButtonVariant.Ghost, icon = Icons.Download) {
                    Text(if (state.split) "Save APKs" else "Save APK")
                }
            }
        },
        bottomBar = if (!finished) null else {
            {
                BottomBar(maxContentWidth = if (layout.twoPane) Dp.Infinity else null) { fill ->
                    Button(onClick = onDone, modifier = fill, size = ButtonSize.Large, variant = ButtonVariant.Ghost) { Text("Done") }
                    if (layout.twoPane) Spacer(Modifier.weight(1f))
                    if (state.phase == RunPhase.Finished) {
                        if (layout.twoPane) {
                            Button(onClick = viewModel::saveArtifact, modifier = fill, size = ButtonSize.Large, variant = ButtonVariant.Ghost, icon = Icons.Download) {
                                Text(if (state.split) "Save APKs" else "Save APK")
                            }
                        }
                        when {
                            state.installed != null -> Button(onClick = viewModel::openInstalled, modifier = fill, size = ButtonSize.Large, icon = Icons.ExternalLink) { Text("Open") }
                            target.installMethod == InstallMethod.MOUNT -> Button(
                                onClick = viewModel::mount,
                                modifier = fill,
                                size = ButtonSize.Large,
                                enabled = !state.mounting,
                                icon = if (state.mounting) null else Icons.Layers,
                            ) {
                                if (state.mounting) Spinner(size = 18)
                                Text("Mount")
                            }
                            else -> Button(onClick = viewModel::openArtifact, modifier = fill, size = ButtonSize.Large, icon = Icons.Download) { Text(artifactActionLabel) }
                        }
                    }
                }
            }
        },
    ) {
        RunContent(state, target, queue, onInstallInstead)
    }
}

@Composable
private fun RunContent(state: RunState, target: PatchTarget, queue: List<String>, onInstallInstead: () -> Unit) {
    val layout = ReseamTheme.layout
    if (layout.twoPane) {
        DesktopRunBody(state, target, queue, onInstallInstead)
    } else {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = layout.pageMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Outcome(state, target, queue, onInstallInstead)
            Queue(queue, state, boxed = state.phase != RunPhase.Running)
            LogDrawer(state.log)
        }
    }
}

/** The result owns the window; the full log is available in its own tab. */
@Composable
private fun DesktopRunBody(state: RunState, target: PatchTarget, queue: List<String>, onInstallInstead: () -> Unit) {
    val layout = ReseamTheme.layout
    val rowHeight = with(LocalDensity.current) { ReseamTheme.typography.bodySmall.lineHeight.toDp() }.coerceAtLeast(28.dp) + 24.dp
    var tab by rememberSaveable { mutableStateOf(ReportTab.Patches) }
    Column(Modifier.fillMaxSize().padding(horizontal = layout.pageMargin, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Outcome(state, target, queue, onInstallInstead)
        TabBar(ReportSegments, tab, { tab = it }, Modifier.fillMaxWidth())
        if (tab == ReportTab.Log) {
            LogPane(state.log, modifier = Modifier.weight(1f).fillMaxWidth())
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(QueueCellMinWidth),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(layout.gutter),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(queue, key = { it }) { reference ->
                    QueueCard(reference, state, boxed = true, modifier = Modifier.height(rowHeight).animateItem())
                }
            }
        }
    }
}

@Composable
private fun Outcome(state: RunState, target: PatchTarget, queue: List<String>, onInstallInstead: () -> Unit) {
    when (state.phase) {
        RunPhase.Running -> Progress(state, target, queue)
        RunPhase.Finished -> Result(state, target, queue)
        RunPhase.Failed -> Failure(state, target)
    }
    UnmountableNotice(state, onInstallInstead)
}

/** A mount build leaves out patches that change the app's manifest; installing as a separate app keeps them. */
@Composable
private fun UnmountableNotice(state: RunState, onInstallInstead: () -> Unit) {
    val names = state.unmountableNames
    if (names.isEmpty()) return
    val list = if (names.size == 1) names.single() else names.dropLast(1).joinToString() + " and " + names.last()
    val works = if (names.size == 1) "works" else "work"
    when (state.phase) {
        RunPhase.Running -> Banner("Leaving out $list, which only $works as a separate app. Patching again.", variant = BannerVariant.Neutral)
        RunPhase.Finished -> Banner(
            message = "Left out $list, which only $works as a separate app.",
            variant = BannerVariant.Neutral,
            trailing = { Button(onClick = onInstallInstead, variant = ButtonVariant.Ghost, size = ButtonSize.Small) { Text("Install instead") } },
        )
        RunPhase.Failed -> Unit
    }
}

@Composable
private fun Progress(state: RunState, target: PatchTarget, queue: List<String>) {
    val motion = ReseamTheme.motion
    val done = queue.count { it in state.statuses }
    val fraction by animateFloatAsState(
        targetValue = if (queue.isEmpty()) 0f else (done + if (state.current != null) 0.5f else 0f) / queue.size,
        animationSpec = motion.tweenSlow(),
        label = "progress",
    )
    if (ReseamTheme.layout.twoPane) {
        DesktopProgress(state, target, done, queue.size, fraction)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressSummary(target, done, queue.size)
            ProgressBar(fraction)
            CurrentPatch(state)
        }
    }
}

/** The overview uses the same tracks as the queue, with no separate content-width cap. */
@Composable
private fun DesktopProgress(state: RunState, target: PatchTarget, done: Int, total: Int, fraction: Float) {
    val gutter = ReseamTheme.layout.gutter
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = ((maxWidth + gutter) / (QueueCellMinWidth + gutter)).toInt().coerceAtLeast(1)
        if (columns >= 3) {
            val cellWidth = (maxWidth - gutter * (columns - 1)) / columns
            val currentSpan = (columns - 1) / 2
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(gutter)) {
                Box(Modifier.width(cellWidth)) { ProgressSummary(target, done, total, showCount = false) }
                CurrentPatchText(state, Modifier.width((cellWidth + gutter) * currentSpan - gutter))
                ProgressTrack(done, total, fraction, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ProgressSummary(target, done, total, showCount = false)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(gutter)) {
                    CurrentPatchText(state, Modifier.weight(1f))
                    ProgressTrack(done, total, fraction, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProgressTrack(done: Int, total: Int, fraction: Float, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Patches applied", style = ReseamTheme.typography.label, color = colors.mutedForeground)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressBar(fraction, Modifier.weight(1f))
            Text("$done / $total", style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
        }
    }
}

@Composable
private fun ProgressSummary(target: PatchTarget, done: Int, total: Int, showCount: Boolean = true) {
    val colors = ReseamTheme.colors
    Row(verticalAlignment = if (ReseamTheme.layout.twoPane) Alignment.Top else Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AppIcon(target.name, target.packageName, size = if (ReseamTheme.layout.twoPane) DesktopAppIconSize else 56.dp, iconPath = target.iconPath)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Patching", style = ReseamTheme.typography.label, color = colors.primary)
            Text(target.name, style = ReseamTheme.typography.title, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showCount) Text("$done / $total", style = ReseamTheme.typography.bodyMedium, color = colors.mutedForeground)
    }
}

@Composable
private fun CurrentPatch(state: RunState, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Card(modifier = modifier.fillMaxWidth(), borderColor = colors.primaryHairline, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CurrentPatchText(state, Modifier.weight(1f))
            Icon(Icons.Sparkles, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CurrentPatchText(state: RunState, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Now applying", style = ReseamTheme.typography.label, color = colors.mutedForeground)
        Text(
            text = state.current?.let(state::patchName) ?: "Preparing the app",
            style = ReseamTheme.typography.bodyMedium,
            color = colors.foreground,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Result(state: RunState, target: PatchTarget, queue: List<String>) {
    val warned = state.failed.isNotEmpty()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (ReseamTheme.layout.twoPane) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ResultBadge(target, warned)
                ResultSummary(state, target, queue, TextAlign.Start, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ResultBadge(target, warned)
                ResultSummary(state, target, queue, TextAlign.Center)
            }
        }
        if (warned) Banner("${state.failed.joinToString(", ") { state.patchName(it) }} failed to apply. Copy the log and share it with the patch author.")
    }
}

@Composable
private fun ResultBadge(target: PatchTarget, warned: Boolean) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val desktop = ReseamTheme.layout.twoPane
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(if (shown) 1f else 0.6f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "badge")
    val alpha by animateFloatAsState(if (shown) 1f else 0f, motion.tweenBase(), label = "badge-alpha")
    Box(
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
        contentAlignment = Alignment.BottomEnd,
    ) {
        AppIcon(target.name, target.packageName, size = if (desktop) DesktopAppIconSize else 64.dp, iconPath = target.iconPath)
        Box(
            modifier = Modifier
                .size(if (desktop) 20.dp else 26.dp)
                .background(colors.surface, CircleShape)
                .padding(2.dp)
                .background(if (warned) colors.warningSoft else colors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (warned) Icons.TriangleAlert else Icons.Check,
                contentDescription = null,
                tint = if (warned) colors.warningForeground else colors.onPrimary,
                modifier = Modifier.size(if (desktop) 12.dp else 15.dp),
            )
        }
    }
}

@Composable
private fun ResultSummary(state: RunState, target: PatchTarget, queue: List<String>, alignment: TextAlign, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignment == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("${target.name} is patched", style = if (ReseamTheme.layout.twoPane) ReseamTheme.typography.title else ReseamTheme.typography.headline, color = colors.foreground, textAlign = alignment, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            text = buildString {
                append("${state.applied} of ${queue.size} patches applied")
                if (state.failed.isNotEmpty()) append(", ${state.failed.size} failed")
                state.durationMs?.let { append(" in ${it / 1000}s") }
            },
            style = if (ReseamTheme.layout.twoPane) ReseamTheme.typography.caption else ReseamTheme.typography.bodySmall,
            color = colors.mutedForeground,
            textAlign = alignment,
        )
    }
}

@Composable
private fun Failure(state: RunState, target: PatchTarget) {
    val colors = ReseamTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(80.dp).background(colors.warningSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.TriangleAlert, null, tint = colors.warningForeground, modifier = Modifier.size(38.dp))
        }
        Text("${target.name} was not patched", style = ReseamTheme.typography.headline, color = colors.foreground, textAlign = TextAlign.Center)
        Banner(state.error ?: "The engine stopped before producing an APK.")
    }
}

@Composable
private fun Queue(queue: List<String>, state: RunState, boxed: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
        SectionHeader("Patches", trailing = queue.size.toString())
        Column(verticalArrangement = Arrangement.spacedBy(if (boxed) 6.dp else 2.dp)) {
            queue.forEach { reference -> QueueCard(reference, state, boxed) }
        }
    }
}

@Composable
private fun QueueCard(reference: String, state: RunState, boxed: Boolean, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    val status = state.statuses[reference]
    val active = state.current == reference && status == null
    Card(
        modifier = modifier.fillMaxWidth(),
        background = when {
            boxed -> colors.surface
            active -> colors.surfaceElevated
            else -> colors.background
        },
        borderColor = when {
            active -> colors.primaryHairline
            boxed -> colors.divider
            else -> colors.background
        },
        shape = ReseamTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusDot(status, active)
            Text(
                text = state.patchName(reference),
                style = ReseamTheme.typography.bodySmall,
                color = if (status != null || active) colors.foreground else colors.mutedForeground,
                modifier = Modifier.weight(1f),
                maxLines = if (ReseamTheme.layout.twoPane) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
            when (status) {
                is PatchStatus.Failed -> Chip("Failed", variant = ChipVariant.Warning)
                is PatchStatus.Skipped -> Chip("Skipped")
                is PatchStatus.Unmountable -> Chip("Left out")
                else -> Unit
            }
        }
    }
}

@Composable
private fun StatusDot(status: PatchStatus?, active: Boolean) {
    val colors = ReseamTheme.colors
    val base = Modifier.size(20.dp).clip(CircleShape)
    when {
        status is PatchStatus.Applied -> Box(base.background(colors.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Check, null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
        }
        status is PatchStatus.Failed -> Box(base.background(colors.warningSoft).border(1.dp, colors.warningHairline, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.TriangleAlert, null, tint = colors.warningForeground, modifier = Modifier.size(13.dp))
        }
        status is PatchStatus.Skipped || status is PatchStatus.Unmountable -> Box(base.background(colors.muted).border(1.dp, colors.divider, CircleShape))
        active -> Box(base.background(colors.primarySoft).border(1.dp, colors.primaryHairline, CircleShape))
        else -> Box(base.background(colors.mutedElevated))
    }
}
