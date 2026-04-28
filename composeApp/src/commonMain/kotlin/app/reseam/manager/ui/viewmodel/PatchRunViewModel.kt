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
import app.reseam.manager.ui.model.PatchEditorState
import app.reseam.manager.ui.model.PatchInput
import app.reseam.manager.ui.model.PatchRunConfig
import app.reseam.manager.ui.model.PatchRunState
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.model.RunStatus
import app.reseam.manager.ui.model.navigation.ManagerRoute
import app.reseam.manager.ui.model.toLogLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
        val input = store.state.flow.selectedInput ?: return
        val editor = store.state.flow.editor
        if (!editor.canPatch) return

        runJob?.cancel()
        runJob = scope.launch {
            store.update {
                it.copy(
                    navigation = it.navigation.push(ManagerRoute.Run),
                    error = null,
                    flow = it.flow.copy(
                        run = PatchRunState(
                            status = RunStatus.Running,
                            progressPercent = 1,
                            currentPatch = editor.patches.firstOrNull { patch -> patch.enabled }?.metadata?.name,
                        ),
                    ),
                )
            }

            when (val result = core.patch(patchPlan(input, editor, config)) { applyRunEvent(it, editor) }) {
                is ReseamCallResult.Success -> finishRun(input, result.value)
                is ReseamCallResult.Failure -> {
                    store.setError(result.message)
                    store.updateFlow { it.copy(run = it.run.copy(status = RunStatus.Failed)) }
                }
            }
        }
    }

    fun install() {
        val artifact = store.state.flow.run.artifact ?: return
        val appInstaller = installer ?: return
        scope.launch {
            runCatching { appInstaller.install(artifact) }
                .onFailure { error -> store.setError(error.message ?: "Install failed") }
        }
    }

    private fun patchPlan(
        input: PatchInput,
        editor: PatchEditorState,
        config: PatchRunConfig?,
    ): PatchPlan =
        PatchPlan(
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
        val run = store.state.flow.run
        val activeCount = editor.patches.count { it.enabled }.coerceAtLeast(1)
        val patchStatuses = when (event) {
            is RunEvent.PatchFinished -> run.patchStatuses + (event.patch to event.status)
            else -> run.patchStatuses
        }
        val currentPatch = when (event) {
            is RunEvent.PatchStarted -> event.patch
            else -> run.currentPatch
        }

        store.updateFlow {
            it.copy(
                run = run.copy(
                    progressPercent = ((patchStatuses.size * 100) / activeCount).coerceIn(1, 99),
                    currentPatch = currentPatch,
                    patchStatuses = patchStatuses,
                    logs = run.logs + event.toLogLine(),
                ),
            )
        }
    }

    private suspend fun finishRun(input: PatchInput, outcome: PatchOutcome) {
        outcome.artifact?.let { artifact ->
            patchedApps.save(
                PatchedAppSummary(
                    id = input.displayName.lowercase().replace(Regex("[^a-z0-9]+"), "-"),
                    name = input.displayName,
                    packageName = store.state.flow.inspect.value?.apk?.packageName.orEmpty(),
                    versionName = store.state.flow.inspect.value?.apk?.versionName,
                    patchCount = outcome.results.count { it.status is PatchStatus.Applied },
                    artifactPath = artifact.path,
                    bundleNames = store.state.bundles.installed.map { it.name },
                ),
            )
        }

        store.updateFlow {
            it.copy(
                run = it.run.copy(
                    status = RunStatus.Finished,
                    progressPercent = 100,
                    currentPatch = null,
                    outcome = outcome,
                    artifact = outcome.artifact,
                ),
            )
        }
    }
}
