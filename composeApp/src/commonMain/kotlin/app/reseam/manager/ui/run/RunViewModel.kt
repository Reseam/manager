package app.reseam.manager.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.AppliedPatch
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.sdk.LogLevel
import app.reseam.manager.sdk.PatchMetadata
import app.reseam.manager.sdk.PatchOutput
import app.reseam.manager.sdk.PatchRequest
import app.reseam.manager.sdk.PatchResult
import app.reseam.manager.sdk.PatchSelection
import app.reseam.manager.sdk.PatchStatus
import app.reseam.manager.sdk.ReseamSdk
import app.reseam.manager.sdk.RunEvent
import app.reseam.manager.ui.components.LogLine
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.userMessage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class RunPhase { Running, Finished, Failed }

data class RunState(
    val phase: RunPhase = RunPhase.Running,
    val patches: Map<String, PatchMetadata> = emptyMap(),
    val current: String? = null,
    val statuses: Map<String, PatchStatus> = emptyMap(),
    /** Set when the run finishes; it carries why each patch ran, which the events do not. */
    val results: List<PatchResult> = emptyList(),
    val log: List<LogLine> = emptyList(),
    val output: String? = null,
    val split: Boolean = false,
    val error: String? = null,
    val durationMs: Long? = null,
) {
    fun patchName(id: String): String = patches[id]?.name ?: id

    /** What the user asked for, without the internals and dependencies that came with it. */
    val applied: Int get() = results.count { it.status is PatchStatus.Applied && it.chosen }
    val failed: List<String> get() = statuses.filterValues { it is PatchStatus.Failed }.keys.toList()
}

@OptIn(ExperimentalTime::class)
class RunViewModel(
    private val graph: AppGraph,
    private val target: PatchTarget,
    private val selection: PatchSelection,
    private val bundlePaths: List<String>,
    patches: List<PatchMetadata>,
) : ViewModel() {
    private val current = MutableStateFlow(RunState(patches = patches.associateBy { it.id }))
    val state: StateFlow<RunState> = current.asStateFlow()

    init {
        viewModelScope.launch { run() }
    }

    private suspend fun run() {
        val packageName = target.packageName ?: target.name.sanitized()
        try {
            val destination = graph.patchedApps.outputPath(packageName)
            val outcome = ReseamSdk.patch(
                PatchRequest(
                    apkPath = target.apkPath,
                    splitPaths = target.splitPaths,
                    bundlePaths = bundlePaths,
                    trust = graph.bundles.trust(),
                    selection = selection,
                    output = PatchOutput.Auto(destination.absolutePath()),
                    signing = graph.signingKeys.files(),
                ),
                onEvent = ::onEvent,
            )
            graph.patchedApps.save(
                PatchedApp(
                    packageName = packageName,
                    name = target.name,
                    versionName = target.versionName,
                    apkPath = outcome.output.path,
                    sourceApkPath = target.apkPath,
                    iconPath = target.iconPath,
                    sourceSplitPaths = target.splitPaths,
                    patches = outcome.results.filter { it.status is PatchStatus.Applied && it.chosen }.map { AppliedPatch(it.name, current.value.patches[it.name]?.bundle.orEmpty(), current.value.patchName(it.name)) },
                    patchedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
                ),
            )
            current.update {
                it.copy(
                    phase = RunPhase.Finished,
                    current = null,
                    statuses = outcome.results.associate { result -> result.name to result.status },
                    results = outcome.results,
                    output = outcome.output.path,
                    split = outcome.output is PatchOutput.SplitDir,
                    durationMs = outcome.metrics.totalDurationMs,
                )
            }
        } catch (error: Exception) {
            current.update { it.copy(phase = RunPhase.Failed, current = null, error = error.userMessage()) }
        } finally {
            graph.signingKeys.refresh()
        }
    }

    fun openArtifact() {
        val path = state.value.output ?: return
        viewModelScope.launch {
            runCatching { graph.artifactAction.run(PlatformFile(path)) }
                .onFailure { graph.notices.post(it.userMessage()) }
        }
    }

    private fun onEvent(event: RunEvent) {
        current.update { state ->
            when (event) {
                is RunEvent.Info -> state.copy(log = state.log + LogLine(LogLevel.Info, null, event.message))
                is RunEvent.PatchStarted -> state.copy(current = event.patch, log = state.log + LogLine(LogLevel.Info, event.patch, "started"))
                is RunEvent.PatchLog -> state.copy(log = state.log + LogLine(event.level, event.patch, event.message))
                is RunEvent.PatchFinished -> state.copy(
                    statuses = state.statuses + (event.patch to event.status),
                    log = state.log + LogLine(
                        level = if (event.status is PatchStatus.Applied) LogLevel.Info else LogLevel.Warn,
                        patch = event.patch,
                        message = when (val status = event.status) {
                            PatchStatus.Applied -> "applied"
                            is PatchStatus.Skipped -> "skipped: ${status.reason}"
                            is PatchStatus.Failed -> "failed: ${status.reason}"
                        },
                    ),
                )
            }
        }
    }
}

private fun String.sanitized(): String = lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-').ifEmpty { "app" }
