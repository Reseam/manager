package app.reseam.manager.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.reseam.manager.data.ResolveProgress
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.HumanCheck
import app.reseam.manager.platform.rememberClipboardCopy
import app.reseam.manager.resources.*
import app.reseam.manager.ui.ArtifactTask
import app.reseam.manager.ui.ReplaceDialog
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.ConfirmDialog
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ProgressRow
import app.reseam.manager.ui.components.ProgressState
import app.reseam.manager.ui.components.RunHeader
import app.reseam.manager.ui.components.RunPhase
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.TopBar
import app.reseam.manager.ui.deliverIcon
import app.reseam.manager.ui.deliverLabel
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.downloadProgress
import app.reseam.manager.ui.duration
import app.reseam.manager.ui.nav.TwoPaneWidth
import app.reseam.manager.ui.shareLabel
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.theme.extendedColors
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.LogLevel
import app.reseam.sdk.PatchStatus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RunScreen(viewModel: RunViewModel, onClose: () -> Unit, onFinished: () -> Unit, onInstallInstead: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var stopping by rememberSaveable { mutableStateOf(false) }
    var showingLog by rememberSaveable { mutableStateOf(false) }
    val stage = state.stage
    val running = stage is RunStage.Preparing || stage == RunStage.Patching
    val onBack = when (stage) {
        is RunStage.Done -> onFinished
        is RunStage.Failed -> onClose
        else -> ({ stopping = true })
    }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), onBackCompleted = onBack)
    LaunchedEffect(running) { if (!running) stopping = false }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
        val wide = maxWidth >= TwoPaneWidth
        val header = @Composable { Header(state, onShowLog = { showingLog = true }.takeIf { stage is RunStage.Failed && !wide && state.log.isNotEmpty() }) }
        val action = @Composable { Action(state, viewModel, onClose, onInstallInstead) }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                    TopBar(null, onBack = onBack)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { header() }
                }
                Column(Modifier.weight(1f).fillMaxSize().background(MaterialTheme.colorScheme.surface).windowInsetsPadding(WindowInsets.statusBars).padding(top = Space.xxl)) {
                    Patches(state, Modifier.weight(1f))
                    Footer(action)
                }
            }
        } else if (running) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                TopBar(null, onBack = onBack)
                header()
                Column(Modifier.weight(1f).clip(RoundedCornerShape(topStart = Radius.md, topEnd = Radius.md)).background(MaterialTheme.colorScheme.surface).padding(top = Space.xxl)) {
                    Patches(state, Modifier.weight(1f))
                    Footer(action)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                TopBar(null, onBack = onBack)
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) { header() }
                Footer(action)
            }
        }
    }

    if (stopping) {
        ConfirmDialog(
            headline = stringResource(Res.string.run_stop_title),
            body = stringResource(Res.string.run_stop_body, state.app.name),
            icon = Icons.Warning,
            confirm = stringResource(Res.string.run_stop),
            onConfirm = onClose,
            onDismiss = { stopping = false },
            dismiss = stringResource(Res.string.run_keep),
        )
    }
    if (viewModel.tasks.conflict.collectAsStateWithLifecycle().value != null) ReplaceDialog(state.app.name, viewModel.tasks::replace, viewModel.tasks::keepInstalled)
    state.humanCheck?.let { url ->
        Sheet(stringResource(Res.string.check_title), onDismiss = viewModel::abandonCheck) {
            Text(stringResource(Res.string.check_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HumanCheck(url, onVerified = viewModel::verified)
        }
    }
    if (showingLog) {
        Sheet(stringResource(Res.string.run_log), onDismiss = { showingLog = false }) { Log(state.log) }
    }
}

@Composable
private fun Header(state: RunState, onShowLog: (() -> Unit)?) {
    val (phase, title, supporting) = when (val stage = state.stage) {
        is RunStage.Preparing -> Triple(
            RunPhase.Working((stage.progress as? ResolveProgress.Downloading)?.let { progress -> progress.total?.let { progress.written.toFloat() / it } }),
            state.app.name,
            when (val progress = stage.progress) {
                ResolveProgress.Preparing -> stringResource(Res.string.run_preparing)
                is ResolveProgress.Downloading -> downloadProgress(progress.written, progress.total)
            },
        )
        RunStage.Patching -> Triple(
            RunPhase.Working(if (state.queue.isEmpty()) null else state.finished.toFloat() / state.queue.size),
            state.app.name,
            stringResource(Res.string.run_applying, state.finished, state.queue.size),
        )
        is RunStage.Done -> Triple(
            RunPhase.Succeeded,
            stringResource(Res.string.run_ready, state.app.name),
            state.patchingTime?.let { pluralStringResource(Res.plurals.run_applied_in, state.applied, state.applied, duration(it)) }
                ?: pluralStringResource(Res.plurals.run_applied, state.applied, state.applied),
        )
        is RunStage.Failed -> Triple(RunPhase.Failed, stringResource(Res.string.run_failed), stage.error.describe())
    }
    RunHeader(state.app, phase, title, supporting) {
        if (state.unmountable.isNotEmpty()) {
            Text(
                text = pluralStringResource(Res.plurals.run_left_out, state.unmountable.size, state.unmountable.size),
                modifier = Modifier.padding(top = Space.sm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (onShowLog != null) Button(stringResource(Res.string.run_show_log), onShowLog, style = ButtonStyle.Text)
    }
}

@Composable
private fun Patches(state: RunState, modifier: Modifier) {
    var logOpen by rememberSaveable { mutableStateOf(false) }
    LazyColumn(modifier.fillMaxWidth()) {
        item { SectionLabel(stringResource(Res.string.app_patches), Modifier.padding(horizontal = Layout.margin)) }
        items(state.queue, key = { it }) { reference ->
            ProgressRow(
                title = state.names[reference] ?: reference,
                state = when (state.statuses[reference]) {
                    PatchStatus.Applied -> ProgressState.Applied
                    is PatchStatus.Failed -> ProgressState.Failed
                    is PatchStatus.Skipped, is PatchStatus.Unmountable -> ProgressState.Skipped
                    null -> if (reference == state.current) ProgressState.Running else ProgressState.Pending
                },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { logOpen = !logOpen }.padding(horizontal = Layout.margin, vertical = Space.sm).padding(top = Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(stringResource(Res.string.run_log), Modifier.weight(1f))
                Chevron(expanded = logOpen)
            }
        }
        if (logOpen) item { Box(Modifier.padding(horizontal = Layout.margin)) { Log(state.log) } }
    }
}

@Composable
private fun Log(lines: List<LogLine>) {
    val copy = rememberClipboardCopy()
    val label = stringResource(Res.string.run_log)
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        if (lines.isEmpty()) Text(stringResource(Res.string.run_log_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        lines.forEach { line ->
            Text(
                text = line.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (line.level == LogLevel.WARN) MaterialTheme.extendedColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (lines.isNotEmpty()) {
            Button(
                label = stringResource(Res.string.run_copy_log),
                onClick = { copy(label, lines.joinToString("\n") { it.text }) },
                style = ButtonStyle.Tonal,
                size = ButtonSize.Medium,
                icon = Icons.Copy,
            )
        }
    }
}

@Composable
private fun Footer(action: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(start = Layout.margin, end = Layout.margin, top = Space.sm, bottom = Space.sm)) { action() }
}

@Composable
private fun Action(state: RunState, viewModel: RunViewModel, onClose: () -> Unit, onInstallInstead: () -> Unit) {
    val busy by viewModel.tasks.busy.collectAsStateWithLifecycle()
    val artifactKind = viewModel.artifactKind
    val modifier = Modifier.fillMaxWidth()
    when (state.stage) {
        is RunStage.Preparing, RunStage.Patching -> Button(stringResource(Res.string.cancel), onClose, modifier, style = ButtonStyle.Outlined, icon = Icons.Close)
        is RunStage.Failed -> Button(stringResource(Res.string.try_again), viewModel::retry, modifier, icon = Icons.Retry)
        is RunStage.Done -> Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            when {
                state.installedAs != null && artifactKind == ArtifactAction.Kind.Install -> Button(stringResource(Res.string.run_open), viewModel::open, modifier, icon = Icons.ExternalLink)
                viewModel.installMethod == InstallMethod.MOUNT -> Button(
                    label = stringResource(if (busy == ArtifactTask.Mount) Res.string.run_mounting else Res.string.run_mount),
                    onClick = viewModel.tasks::mount,
                    modifier = modifier,
                    icon = Icons.Layers,
                    enabled = busy == null,
                    loading = busy == ArtifactTask.Mount,
                )
                else -> Button(
                    label = artifactKind.deliverLabel(busy = busy == ArtifactTask.Deliver),
                    onClick = viewModel.tasks::deliver,
                    modifier = modifier,
                    icon = artifactKind.deliverIcon,
                    enabled = busy == null,
                    loading = busy == ArtifactTask.Deliver,
                )
            }
            Button(
                label = artifactKind.shareLabel(),
                onClick = viewModel.tasks::share,
                modifier = modifier,
                style = ButtonStyle.Text,
                enabled = busy == null,
                loading = busy == ArtifactTask.Share,
            )
            if (state.unmountable.isNotEmpty() && state.installedAs == null) {
                Button(stringResource(Res.string.run_install_instead), onInstallInstead, modifier, style = ButtonStyle.Text)
            }
        }
    }
}
