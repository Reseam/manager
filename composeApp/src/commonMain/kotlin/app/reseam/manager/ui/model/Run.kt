package app.reseam.manager.ui.model

import app.reseam.manager.patcher.PatchArtifact
import app.reseam.manager.patcher.PatchOutcome
import app.reseam.manager.patcher.PatchOutput
import app.reseam.manager.patcher.PatchRunStatus
import app.reseam.manager.patcher.RunEvent
import app.reseam.manager.patcher.SigningConfig

data class PatchRunState(
    val status: RunStatus = RunStatus.Idle,
    val progressPercent: Int = 0,
    val currentPatch: String? = null,
    val patchStatuses: Map<String, PatchRunStatus> = emptyMap(),
    val logs: List<RunLogLine> = emptyList(),
    val outcome: PatchOutcome? = null,
    val artifact: PatchArtifact? = null,
) {
    val hasFailure: Boolean
        get() = patchStatuses.values.any { it == PatchRunStatus.Failed }
}

enum class RunStatus {
    Idle,
    Running,
    Finished,
    Failed,
}

data class RunLogLine(
    val level: String,
    val message: String,
    val patch: String? = null,
)

data class PatchRunConfig(
    val output: PatchOutput,
    val signing: SigningConfig = SigningConfig(),
    val dryRun: Boolean = false,
)

fun RunEvent.toLogLine(): RunLogLine =
    when (this) {
        is RunEvent.Info -> RunLogLine(level = "info", message = message)
        is RunEvent.PatchStarted -> RunLogLine(level = "info", message = "Applying patch: $patch", patch = patch)
        is RunEvent.PatchFinished -> RunLogLine(
            level = if (status == PatchRunStatus.Failed) "error" else "info",
            message = reason ?: "Patch $status",
            patch = patch,
        )
        is RunEvent.PatchLog -> RunLogLine(level = level, message = message, patch = patch)
    }
