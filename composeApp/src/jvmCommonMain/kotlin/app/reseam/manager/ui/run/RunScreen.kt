package app.reseam.manager.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import app.reseam.manager.platform.HumanCheck
import app.reseam.manager.resources.*
import app.reseam.manager.ui.ReplaceDialog
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.ConfirmDialog
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.LazySheet
import app.reseam.manager.ui.components.RunHeader
import app.reseam.manager.ui.components.RunPhase
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.TopBar
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.downloadProgress
import app.reseam.manager.ui.duration
import app.reseam.manager.ui.nav.TwoPaneWidth
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Space
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
        val finished = !running
        val header = @Composable { Header(state, onShowLog = { showingLog = true }.takeIf { finished && !wide && state.log.isNotEmpty() }) }
        val action = @Composable { RunAction(state, viewModel, onClose, onInstallInstead) }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                    TopBar(null, onBack = onBack)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { header() }
                }
                Column(Modifier.weight(1f).fillMaxSize().background(MaterialTheme.colorScheme.surface).windowInsetsPadding(WindowInsets.statusBars).padding(top = Space.xxl)) {
                    RunPatches(state, Modifier.weight(1f))
                    RunFooter(action)
                }
            }
        } else if (running) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                TopBar(null, onBack = onBack)
                header()
                Column(Modifier.weight(1f).clip(RoundedCornerShape(topStart = Radius.md, topEnd = Radius.md)).background(MaterialTheme.colorScheme.surface).padding(top = Space.xxl)) {
                    RunPatches(state, Modifier.weight(1f))
                    RunFooter(action)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                TopBar(null, onBack = onBack)
                Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) { header() }
                RunFooter(action)
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
    viewModel.tasks.pending.collectAsStateWithLifecycle().value?.let { ReplaceDialog(state.app.name, it.conflict.newer, viewModel.tasks::replace, viewModel.tasks::keepInstalled) }
    state.humanCheck?.let { url ->
        Sheet(stringResource(Res.string.check_title), onDismiss = viewModel::abandonCheck) {
            Text(stringResource(Res.string.check_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HumanCheck(url, onVerified = viewModel::verified)
        }
    }
    if (showingLog) {
        LazySheet(stringResource(Res.string.run_log), onDismiss = { showingLog = false }, action = { CopyLogButton(state.log) }) { runLog(state.log) }
    }
}

@Composable
private fun Header(state: RunState, onShowLog: (() -> Unit)?) {
    val (phase, title, supporting) = when (val stage = state.stage) {
        is RunStage.Preparing -> Triple(
            RunPhase.Working((stage.progress as? ResolveProgress.Downloading)?.transfer?.fraction),
            state.app.name,
            when (val progress = stage.progress) {
                ResolveProgress.Preparing -> stringResource(Res.string.run_preparing)
                is ResolveProgress.Downloading -> downloadProgress(progress.transfer)
            },
        )
        RunStage.Patching -> Triple(
            RunPhase.Working(if (state.queue.isEmpty()) null else state.finished.toFloat() / state.queue.size),
            state.app.name,
            stringResource(Res.string.run_applying, state.finished, state.queue.size),
        )
        is RunStage.Done -> Triple(
            if (state.leftOut.isEmpty()) RunPhase.Succeeded else RunPhase.Partial,
            stringResource(Res.string.run_ready, state.app.name),
            state.patchingTime?.let { pluralStringResource(Res.plurals.run_applied_in, state.applied, state.applied, duration(it)) }
                ?: pluralStringResource(Res.plurals.run_applied, state.applied, state.applied),
        )
        is RunStage.Failed -> Triple(RunPhase.Failed, stringResource(Res.string.run_failed), stage.error.describe())
    }
    RunHeader(state.app, phase, title, supporting) {
        if (state.stage is RunStage.Done && state.leftOut.isNotEmpty()) {
            Text(
                text = pluralStringResource(Res.plurals.run_failed_left_out, state.leftOut.size, state.leftOut.size, state.leftOut.joinToString { state.names[it] ?: it }),
                modifier = Modifier.padding(top = Space.sm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
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
