package app.reseam.manager.patcher

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class InspectRequest(
    @SerialName("apk_path")
    val apkPath: String? = null,
    @SerialName("split_paths")
    val splitPaths: List<String> = emptyList(),
    @SerialName("bundle_paths")
    val bundlePaths: List<String> = emptyList(),
    @SerialName("include_builtin_trust")
    val includeBuiltinTrust: Boolean = true,
    @SerialName("trusted_public_keys_hex")
    val trustedPublicKeysHex: List<String> = emptyList(),
)

@Serializable
data class InspectResponse(
    val apk: ApkMetadata? = null,
    val bundles: List<BundleMetadata> = emptyList(),
    val patches: List<PatchMetadata> = emptyList(),
    @SerialName("requires_trust")
    val requiresTrust: Boolean = false,
)

@Serializable
data class ApkMetadata(
    @SerialName("package_name")
    val packageName: String? = null,
    @SerialName("version_name")
    val versionName: String? = null,
    @SerialName("version_code")
    val versionCode: Long? = null,
    @SerialName("dex_files")
    val dexFiles: Int = 0,
    @SerialName("component_count")
    val componentCount: Int = 0,
    @SerialName("split_names")
    val splitNames: List<String> = emptyList(),
    @SerialName("class_count")
    val classCount: Int = 0,
    @SerialName("method_count")
    val methodCount: Int = 0,
)

@Serializable
data class BundleMetadata(
    @SerialName("file_name")
    val fileName: String,
    val name: String,
    val author: String,
    val description: String,
    @SerialName("extension_dex")
    val extensionDex: List<String> = emptyList(),
    @SerialName("signer_public_key_hex")
    val signerPublicKeyHex: String,
    @SerialName("signer_fingerprint")
    val signerFingerprint: String,
    @SerialName("trust_status")
    val trustStatus: TrustStatus,
)

@Serializable
enum class TrustStatus {
    @SerialName("trusted")
    Trusted,

    @SerialName("unknown")
    Unknown,
}

@Serializable
data class PatchMetadata(
    @SerialName("source_bundle")
    val sourceBundle: String,
    val name: String,
    val description: String,
    @SerialName("enabled_by_default")
    val enabledByDefault: Boolean,
    val dependencies: List<String> = emptyList(),
    @SerialName("compatible_with")
    val compatibleWith: List<CompatibilityMetadata> = emptyList(),
    val options: List<OptionMetadata> = emptyList(),
    @SerialName("is_compatible")
    val isCompatible: Boolean,
    @SerialName("incompatibility_reason")
    val incompatibilityReason: String? = null,
)

@Serializable
data class CompatibilityMetadata(
    @SerialName("package_name")
    val packageName: String,
    val versions: List<String> = emptyList(),
)

@Serializable
data class OptionMetadata(
    val key: String,
    val title: String,
    val description: String,
    @SerialName("option_type")
    val optionType: OptionKind,
    @SerialName("default_value")
    val defaultValue: InputOptionValue? = null,
    @SerialName("valid_values")
    val validValues: List<String>? = null,
    val required: Boolean,
)

@Serializable
enum class OptionKind {
    @SerialName("string")
    String,

    @SerialName("bool")
    Bool,

    @SerialName("int")
    Int,

    @SerialName("float")
    Float,

    @SerialName("string_list")
    StringList,

    @SerialName("path")
    Path,
}

@Serializable
sealed interface InputOptionValue {
    @Serializable
    @SerialName("string")
    data class StringValue(val value: String) : InputOptionValue

    @Serializable
    @SerialName("bool")
    data class BoolValue(val value: Boolean) : InputOptionValue

    @Serializable
    @SerialName("int")
    data class IntValue(val value: Long) : InputOptionValue

    @Serializable
    @SerialName("float")
    data class FloatValue(val value: Double) : InputOptionValue

    @Serializable
    @SerialName("string_list")
    data class StringListValue(val value: List<String>) : InputOptionValue

    @Serializable
    @SerialName("path")
    data class PathValue(val value: String) : InputOptionValue
}

@Serializable
data class PatchSelection(
    val enable: List<String> = emptyList(),
    val disable: List<String> = emptyList(),
    val options: Map<String, Map<String, InputOptionValue>> = emptyMap(),
)

@Serializable
data class PatchRequest(
    @SerialName("apk_path")
    val apkPath: String,
    @SerialName("split_paths")
    val splitPaths: List<String> = emptyList(),
    @SerialName("bundle_paths")
    val bundlePaths: List<String> = emptyList(),
    val output: PatchOutput,
    val selection: PatchSelection = PatchSelection(),
    @SerialName("include_builtin_trust")
    val includeBuiltinTrust: Boolean = true,
    @SerialName("trusted_public_keys_hex")
    val trustedPublicKeysHex: List<String> = emptyList(),
    @SerialName("key_path")
    val keyPath: String? = null,
    @SerialName("cert_path")
    val certPath: String? = null,
    @SerialName("dry_run")
    val dryRun: Boolean = false,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("kind")
sealed interface PatchOutput {
    @Serializable
    @SerialName("single_file")
    data class SingleFile(val path: String) : PatchOutput

    @Serializable
    @SerialName("split_dir")
    data class SplitDir(val path: String) : PatchOutput
}

@Serializable
data class PatchOutcome(
    val results: List<PatchResult> = emptyList(),
    val artifact: PatchArtifact? = null,
    val metrics: PatchMetrics = PatchMetrics(),
)

@Serializable
data class PatchResult(
    val name: String,
    val status: PatchStatus,
    val logs: List<LogEntry> = emptyList(),
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("kind")
sealed interface PatchStatus {
    @Serializable
    @SerialName("applied")
    data object Applied : PatchStatus

    @Serializable
    @SerialName("skipped")
    data class Skipped(val reason: String) : PatchStatus

    @Serializable
    @SerialName("failed")
    data class Failed(val reason: String) : PatchStatus
}

@Serializable
data class LogEntry(
    val level: String,
    val patch: String,
    val message: String,
)

@Serializable
data class PatchArtifact(
    val kind: ArtifactKind,
    val path: String,
)

@Serializable
enum class ArtifactKind {
    @SerialName("apk")
    Apk,

    @SerialName("split_directory")
    SplitDirectory,
}

@Serializable
data class PatchMetrics(
    @SerialName("total_duration_ms")
    val totalDurationMs: Long = 0,
    @SerialName("final_rss_bytes")
    val finalRssBytes: Long? = null,
    @SerialName("peak_rss_bytes")
    val peakRssBytes: Long? = null,
    val phases: List<PatchPhaseMetrics> = emptyList(),
)

@Serializable
data class PatchPhaseMetrics(
    val phase: PatchPhase,
    @SerialName("duration_ms")
    val durationMs: Long,
    @SerialName("rss_bytes")
    val rssBytes: Long? = null,
    @SerialName("peak_rss_bytes")
    val peakRssBytes: Long? = null,
)

@Serializable
enum class PatchPhase {
    @SerialName("open_apk")
    OpenApk,

    @SerialName("load_bundles")
    LoadBundles,

    @SerialName("compile_selection")
    CompileSelection,

    @SerialName("validate_patches")
    ValidatePatches,

    @SerialName("apply_patches")
    ApplyPatches,

    @SerialName("write_unsigned_artifacts")
    WriteUnsignedArtifacts,

    @SerialName("load_signing_key")
    LoadSigningKey,

    @SerialName("sign_artifacts")
    SignArtifacts,
}

@Serializable
sealed interface RunEvent {
    @Serializable
    @SerialName("info")
    data class Info(val message: String) : RunEvent

    @Serializable
    @SerialName("patch_started")
    data class PatchStarted(val patch: String) : RunEvent

    @Serializable
    @SerialName("patch_finished")
    data class PatchFinished(
        val patch: String,
        val status: PatchRunStatus,
        val reason: String? = null,
    ) : RunEvent

    @Serializable
    @SerialName("patch_log")
    data class PatchLog(
        val patch: String,
        val level: String,
        val message: String,
    ) : RunEvent
}

@Serializable
enum class PatchRunStatus {
    @SerialName("applied")
    Applied,

    @SerialName("skipped")
    Skipped,

    @SerialName("failed")
    Failed,
}
