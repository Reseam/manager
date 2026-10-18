package app.reseam.manager.ui.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.PatchJob
import app.reseam.manager.data.Failure
import app.reseam.manager.data.PatchedAppKey
import app.reseam.manager.data.ResolvedApk
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.resources.*
import app.reseam.manager.sdk.reference
import app.reseam.manager.ui.ArtifactTasks
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.lookOf
import app.reseam.manager.ui.nav.Route
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.RunEvent
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private const val StartedKey = "started"

class RunViewModel(private val graph: AppGraph, private val route: Route.Run, private val saved: SavedStateHandle) : ViewModel() {
    private val packageName = route.packageName
    private val current = MutableStateFlow(RunState(AppLook(packageName, packageName)))
    val state: StateFlow<RunState> = current.asStateFlow()

    private var job: Job? = null
    private var resolved: ResolvedApk? = null

    val installMethod: InstallMethod = if (route.mount) InstallMethod.MOUNT else InstallMethod.INSTALL
    val build = PatchedAppKey(packageName, installMethod)
    val artifactKind: ArtifactAction.Kind = graph.actions.kind

    val tasks = ArtifactTasks(viewModelScope, graph.notices, graph.actions, graph.patchedApps) { installed ->
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
        val app = graph.patchedApps.find(build).first()?.takeIf { it.patchedAtEpochMs >= route.startedAtEpochMs }
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
            graph.backgroundRun?.hold(current.map { it.app.name }.distinctUntilChanged().map { getString(Res.string.notify_patching_title, it) }) { run() } ?: run()
            notify()
        }
    }

    private suspend fun run() {
        try {
            val apk = resolved ?: graph.apkSources.resolve(packageName, route.source) { progress ->
                current.update { it.copy(stage = RunStage.Preparing(progress)) }
            }.also { resolved = it }
            current.update { state -> state.copy(stage = RunStage.Patching, app = apk.identity?.let { AppLook(it.name, packageName, it.iconPath) } ?: state.app) }
            val app = current.value.app
            val started = TimeSource.Monotonic.markNow()
            val build = graph.patcher.patch(PatchJob(packageName, app.name, app.iconPath, route.source, apk, route.selection, installMethod), ::onEvent)
            val patchingTime = started.elapsedNow()
            current.update { it.finished(build.output, build.outcome.results, patchingTime) }
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
        current.update { it.after(event) }
    }
}
