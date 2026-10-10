package app.reseam.manager.ui.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.PatchJob
import app.reseam.manager.data.Failure
import app.reseam.manager.data.ResolveProgress
import app.reseam.manager.data.ResolvedApk
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.resources.*
import app.reseam.manager.sdk.chosen
import app.reseam.manager.sdk.reference
import app.reseam.manager.ui.ArtifactTasks
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.lookOf
import app.reseam.manager.ui.nav.Route
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.LogLevel
import app.reseam.sdk.PatchStatus
import app.reseam.sdk.RunEvent
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private const val StartedKey = "started"

data class LogLine(val level: LogLevel, val patch: String?, val message: String) {
    val text: String get() = listOfNotNull(patch, message).joinToString(": ")
}

sealed interface RunStage {
    data class Preparing(val progress: ResolveProgress) : RunStage
    data object Patching : RunStage
    data class Done(val output: String) : RunStage
    data class Failed(val error: Throwable) : RunStage
}

data class RunState(
    val app: AppLook,
    val stage: RunStage = RunStage.Preparing(ResolveProgress.Preparing),
    val queue: List<String> = emptyList(),
    val names: Map<String, String> = emptyMap(),
    val current: String? = null,
    val statuses: Map<String, PatchStatus> = emptyMap(),
    val applied: Int = 0,
    val unmountable: List<String> = emptyList(),
    val log: List<LogLine> = emptyList(),
    val humanCheck: String? = null,
    val installedAs: String? = null,
    val patchingTime: Duration? = null,
) {
    val finished: Int get() = queue.count { it in statuses }
}

class RunViewModel(private val graph: AppGraph, private val route: Route.Run, private val saved: SavedStateHandle) : ViewModel() {
    private val packageName = route.packageName
    private val current = MutableStateFlow(RunState(AppLook(packageName, packageName)))
    val state: StateFlow<RunState> = current.asStateFlow()

    private var job: Job? = null
    private var resolved: ResolvedApk? = null

    val installMethod: InstallMethod = if (route.mount) InstallMethod.MOUNT else InstallMethod.INSTALL
    val artifactKind: ArtifactAction.Kind = graph.actions.kind

    val tasks = ArtifactTasks(viewModelScope, graph.notices, graph.actions, graph.patchedApps, packageName) { installed ->
        installed?.let { current.update { state -> state.copy(installedAs = it) } }
    }

    init {
        viewModelScope.launch {
            val patches = graph.bundles.patches.filterNotNull().first().values.flatten().associateBy { it.reference }
            current.update { state ->
                state.copy(
                    app = graph.lookOf(packageName),
                    queue = route.selection.enable,
                    names = route.selection.enable.associateWith { patches[it]?.spec?.name ?: it },
                )
            }
            if (saved.get<Boolean>(StartedKey) == true) restore() else start()
            saved[StartedKey] = true
        }
    }

    /** The process died during or after the run; the library says whether it finished. */
    private suspend fun restore() {
        val app = graph.patchedApps.find(packageName).first()?.takeIf { it.patchedAtEpochMs >= route.startedAtEpochMs }
        current.update {
            if (app == null) it.copy(stage = RunStage.Failed(Failure.Interrupted())) else it.copy(stage = RunStage.Done(app.apkPath), applied = app.patches.size, installedAs = app.installedAs)
        }
    }

    fun retry() {
        current.update { RunState(it.app, queue = it.queue, names = it.names) }
        start()
    }

    fun verified() {
        current.update { it.copy(humanCheck = null) }
        start()
    }

    fun abandonCheck() {
        current.update { state -> state.copy(stage = RunStage.Failed(Failure.HumanCheckRequired(checkNotNull(state.humanCheck))), humanCheck = null) }
    }

    private fun start() {
        job?.cancel()
        job = viewModelScope.launch {
            graph.backgroundRun?.hold { run() } ?: run()
            notify()
        }
    }

    private suspend fun run() {
        try {
            val apk = resolved ?: graph.apkSources.resolve(packageName, route.source) { progress ->
                current.update { it.copy(stage = RunStage.Preparing(progress)) }
            }.also { resolved = it }
            current.update { it.copy(stage = RunStage.Patching) }
            val app = current.value.app
            val started = TimeSource.Monotonic.markNow()
            val build = graph.patcher.patch(PatchJob(packageName, app.name, app.iconPath, route.source, apk, route.selection, installMethod), ::onEvent)
            val patchingTime = started.elapsedNow()
            val results = build.outcome.results
            current.update {
                it.copy(
                    stage = RunStage.Done(build.output),
                    current = null,
                    patchingTime = patchingTime,
                    statuses = results.associate { result -> result.patch to result.status },
                    applied = results.count { result -> result.status is PatchStatus.Applied && result.chosen },
                    unmountable = results.filter { result -> result.status is PatchStatus.Unmountable && !result.hidden }.map { result -> result.patch },
                )
            }
        } catch (check: Failure.HumanCheckRequired) {
            current.update { it.copy(humanCheck = check.url) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            current.update { it.copy(stage = RunStage.Failed(error), current = null) }
        }
    }

    private suspend fun notify() {
        val state = current.value
        val background = graph.backgroundRun ?: return
        when (state.stage) {
            is RunStage.Done -> background.finished(getString(Res.string.notify_ready_title, state.app.name), getString(Res.string.notify_ready_body))
            is RunStage.Failed -> background.finished(getString(Res.string.notify_failed_title, state.app.name), getString(Res.string.notify_failed_body))
            else -> Unit
        }
    }

    fun open() {
        current.value.installedAs?.let(graph.actions::open)
    }

    private fun onEvent(event: RunEvent) {
        current.update { state ->
            when (event) {
                is RunEvent.Info -> state.copy(log = state.log + LogLine(LogLevel.INFO, null, event.message))
                is RunEvent.PatchStarted -> state.copy(current = event.patch.takeIf { it in state.names } ?: state.current)
                is RunEvent.Restarted -> state.copy(current = null, statuses = emptyMap(), unmountable = event.unmountable)
                is RunEvent.PatchLog -> state.copy(log = state.log + LogLine(event.field0.level, state.names[event.field0.patch], event.field0.message))
                is RunEvent.PatchFinished -> {
                    val reason = when (val status = event.status) {
                        PatchStatus.Applied -> null
                        is PatchStatus.Skipped -> status.reason
                        is PatchStatus.Unmountable -> status.reason
                        is PatchStatus.Failed -> status.reason
                    }
                    state.copy(
                        statuses = state.statuses + (event.patch to event.status),
                        log = reason?.let { state.log + LogLine(LogLevel.WARN, state.names[event.patch] ?: event.patch, it) } ?: state.log,
                    )
                }
            }
        }
    }
}
