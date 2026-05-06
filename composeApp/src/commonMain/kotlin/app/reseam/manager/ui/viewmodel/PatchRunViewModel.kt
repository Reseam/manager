package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.installer.PatchedAppInstaller
import app.reseam.manager.domain.manager.OutputPathProvider
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.patcher.PatchOutcome
import app.reseam.manager.patcher.PatchOutput
import app.reseam.manager.patcher.PatchPlan
import app.reseam.manager.patcher.PatchStatus
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.patcher.ReseamManagerCore
import app.reseam.manager.patcher.RunEvent
import app.reseam.manager.patcher.SigningConfig
import app.reseam.manager.ui.model.AppView
import app.reseam.manager.ui.model.PatchEditorState
import app.reseam.manager.ui.model.PatchInput
import app.reseam.manager.ui.model.PatchRunConfig
import app.reseam.manager.ui.model.PatchRunState
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.model.RunStatus
import app.reseam.manager.ui.model.toLogLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@Stable
class PatchRunViewModel internal constructor(
    private val store: ManagerStateStore,
    private val core: ReseamManagerCore,
    private val patchedApps: PatchedAppStore,
    private val installer: PatchedAppInstaller?,
    private val outputPaths: OutputPathProvider,
    private val scope: CoroutineScope,
) {
    private var runJob: Job? = null

    fun run(config: PatchRunConfig? = null) {
        val editing = store.state.view as? AppView.Flow.Editing ?: return
        if (!editing.editor.canPatch) return

        runJob?.cancel()
        runJob = scope.launch {
            val initial = AppView.Flow.Running(
                input = editing.input,
                editor = editing.editor,
                run = PatchRunState(
                    status = RunStatus.Running,
                    progressPercent = 1,
                    currentPatch = editing.editor.patches.firstOrNull { it.enabled }?.metadata?.name,
                ),
            )
            store.update {
                it.copy(backStack = it.backStack + initial, error = null)
            }
            val events = Channel<RunEvent>(Channel.UNLIMITED)
            val drain = launch {
                for (event in events) applyRunEvent(event, editing.editor)
            }
            val result = try {
                core.patch(plan(editing.input, editing.editor, config)) { event ->
                    events.trySend(event)
                }
            } finally {
                events.close()
                drain.join()
            }
            when (result) {
                is ReseamCallResult.Success -> finishRun(editing.input, editing.editor, result.value)
                is ReseamCallResult.Failure -> failRun(result.message)
            }
        }
    }

    fun install() {
        val running = store.state.view as? AppView.Flow.Running ?: return
        val artifact = running.run.artifact ?: return
        val appInstaller = installer ?: return
        scope.launch {
            runCatching { appInstaller.install(artifact) }
                .onFailure { error ->
                    store.update { it.copy(error = error.message ?: "Install failed") }
                }
        }
    }

    private fun plan(
        input: PatchInput,
        editor: PatchEditorState,
        config: PatchRunConfig?,
    ): PatchPlan = PatchPlan(
        apkPath = input.apkPath,
        splitPaths = input.splitPaths,
        bundlePaths = store.state.bundles.installed.mapNotNull { it.path },
        output = config?.output ?: PatchOutput.SingleFile(outputPaths.outputFor(input)),
        selection = editor.toSelection(),
        trust = store.state.bundles.toTrustConfig(),
        signing = config?.signing ?: SigningConfig(),
        dryRun = config?.dryRun ?: false,
    )

    private fun applyRunEvent(event: RunEvent, editor: PatchEditorState) {
        val activeCount = editor.patches.count { it.enabled }.coerceAtLeast(1)
        store.update { state ->
            val running = state.view as? AppView.Flow.Running ?: return@update state
            val run = running.run
            val patchStatuses = when (event) {
                is RunEvent.PatchFinished -> run.patchStatuses + (event.patch to event.status)
                else -> run.patchStatuses
            }
            val nextRun = run.copy(
                progressPercent = ((patchStatuses.size * 100) / activeCount).coerceIn(1, 99),
                currentPatch = if (event is RunEvent.PatchStarted) event.patch else run.currentPatch,
                patchStatuses = patchStatuses,
                logs = run.logs + event.toLogLine(),
            )
            state.copy(backStack = state.backStack.dropLast(1) + running.copy(run = nextRun))
        }
    }

    private suspend fun finishRun(input: PatchInput, editor: PatchEditorState, outcome: PatchOutcome) {
        outcome.artifact?.let { artifact ->
            val packageName = editor.inspect?.apk?.packageName.orEmpty()
            patchedApps.save(
                PatchedAppSummary(
                    id = packageName.ifBlank { input.displayName }.lowercase().replace(Regex("[^a-z0-9._-]+"), "-"),
                    name = input.displayName,
                    packageName = packageName,
                    versionName = editor.inspect?.apk?.versionName,
                    patchCount = outcome.results.count { it.status is PatchStatus.Applied },
                    artifactPath = artifact.path,
                    bundleNames = store.state.bundles.installed.map { it.name },
                ),
            )
        }
        store.update { state ->
            val running = state.view as? AppView.Flow.Running ?: return@update state
            val nextRun = running.run.copy(
                status = RunStatus.Finished,
                progressPercent = 100,
                currentPatch = null,
                outcome = outcome,
                artifact = outcome.artifact,
            )
            state.copy(backStack = state.backStack.dropLast(1) + running.copy(run = nextRun))
        }
    }

    private fun failRun(message: String) {
        store.update { state ->
            val running = state.view as? AppView.Flow.Running ?: return@update state
            state.copy(
                backStack = state.backStack.dropLast(1) +
                    running.copy(run = running.run.copy(status = RunStatus.Failed)),
                error = message,
            )
        }
    }
}
