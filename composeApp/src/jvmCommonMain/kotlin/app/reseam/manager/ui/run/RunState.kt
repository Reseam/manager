package app.reseam.manager.ui.run

import app.reseam.manager.data.ResolveProgress
import app.reseam.manager.sdk.chosen
import app.reseam.manager.ui.components.AppLook
import app.reseam.sdk.LogLevel
import app.reseam.sdk.PatchResult
import app.reseam.sdk.PatchStatus
import app.reseam.sdk.RunEvent
import kotlin.time.Duration

data class LogLine(val patch: String?, val message: String) {
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
    /** Chosen patches that couldn't be applied, so the build is without them. */
    val leftOut: List<String> = emptyList(),
    val log: List<LogLine> = emptyList(),
    val humanCheck: String? = null,
    val installedAs: String? = null,
    val patchingTime: Duration? = null,
) {
    val finished: Int get() = queue.count { it in statuses }
}

fun RunState.finished(output: String, results: List<PatchResult>, patchingTime: Duration): RunState = copy(
    stage = RunStage.Done(output),
    current = null,
    patchingTime = patchingTime,
    statuses = results.associate { it.patch to it.status },
    applied = results.count { it.status is PatchStatus.Applied && it.chosen },
    unmountable = results.filter { it.status is PatchStatus.Unmountable && !it.hidden }.map { it.patch },
    leftOut = results.filter { it.chosen && it.status !is PatchStatus.Applied && it.status !is PatchStatus.Unmountable }.map { it.patch },
)

fun RunState.after(event: RunEvent): RunState = when (event) {
    is RunEvent.Info -> this
    is RunEvent.PatchStarted -> copy(current = event.patch.takeIf { it in names } ?: current)
    is RunEvent.PatchesReverted -> copy(current = null, statuses = statuses - event.patches.toSet())
    is RunEvent.PatchLog -> if (event.field0.level == LogLevel.WARN) {
        copy(log = log + LogLine(names[event.field0.patch], event.field0.message))
    } else {
        this
    }
    is RunEvent.PatchFinished -> {
        val reason = when (val status = event.status) {
            PatchStatus.Applied -> null
            is PatchStatus.Skipped -> status.reason
            is PatchStatus.Unmountable -> status.reason
            is PatchStatus.Failed -> status.reason
        }
        copy(
            statuses = statuses + (event.patch to event.status),
            log = reason?.let { log + LogLine(names[event.patch] ?: event.patch, it) } ?: log,
        )
    }
}
