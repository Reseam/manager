package app.reseam.manager.sdk

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class PatchSelection(
    val enable: Set<String> = emptySet(),
    val disable: Set<String> = emptySet(),
    val options: Map<String, Map<String, OptionValue>> = emptyMap(),
    /** Run patches on app versions they were not declared for. The package check still applies. */
    val ignoreVersions: Boolean = false,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed interface PatchOutput {
    val path: String

    @Serializable @SerialName("auto") data class Auto(override val path: String) : PatchOutput
    @Serializable @SerialName("single_file") data class SingleFile(override val path: String) : PatchOutput
    @Serializable @SerialName("split_dir") data class SplitDir(override val path: String) : PatchOutput
}

@Serializable
data class SigningKeyFiles(val key: String, val cert: String)

@Serializable
data class PatchRequest(
    val apkPath: String,
    val splitPaths: List<String> = emptyList(),
    val bundlePaths: List<String>,
    val trust: Trust = Trust(),
    val selection: PatchSelection = PatchSelection(),
    val output: PatchOutput,
    val signing: SigningKeyFiles? = null,
    val dryRun: Boolean = false,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed interface PatchStatus {
    @Serializable @SerialName("applied") data object Applied : PatchStatus
    @Serializable @SerialName("skipped") data class Skipped(val reason: String) : PatchStatus
    @Serializable @SerialName("failed") data class Failed(val reason: String) : PatchStatus
}

@Serializable
enum class LogLevel {
    @SerialName("DEBUG") Debug,
    @SerialName("INFO") Info,
    @SerialName("WARN") Warn,
}

@Serializable
data class LogEntry(val level: LogLevel, val patch: String, val message: String)

@Serializable
data class PatchResult(
    val patch: String,
    /** Internal patches run as dependencies and are never listed to users. */
    val hidden: Boolean = false,
    /** References of the running patches that pulled this one in, empty when the user asked for it directly. */
    val requiredBy: List<String> = emptyList(),
    val status: PatchStatus,
    val logs: List<LogEntry> = emptyList(),
) {
    /** Work the user asked for, rather than a dependency that came along with it. */
    val chosen: Boolean get() = !hidden && requiredBy.isEmpty()
}

@Serializable
enum class PatchPhase {
    @SerialName("open_apk") OpenApk,
    @SerialName("load_bundles") LoadBundles,
    @SerialName("validate_patches") ValidatePatches,
    @SerialName("apply_patches") ApplyPatches,
    @SerialName("write_unsigned_artifacts") WriteUnsignedArtifacts,
    @SerialName("load_signing_key") LoadSigningKey,
    @SerialName("sign_artifacts") SignArtifacts,
}

@Serializable
data class PatchPhaseMetrics(val phase: PatchPhase, val durationMs: Long)

@Serializable
data class PatchMetrics(val totalDurationMs: Long, val phases: List<PatchPhaseMetrics> = emptyList())

@Serializable
data class PatchOutcome(val results: List<PatchResult>, val metrics: PatchMetrics, val output: PatchOutput)

@Serializable
sealed interface RunEvent {
    @Serializable @SerialName("info") data class Info(val message: String) : RunEvent
    @Serializable @SerialName("patch_started") data class PatchStarted(val patch: String) : RunEvent
    @Serializable @SerialName("patch_log") data class PatchLog(val level: LogLevel, val patch: String, val message: String) : RunEvent
    @Serializable @SerialName("patch_finished") data class PatchFinished(val patch: String, val status: PatchStatus) : RunEvent
}
