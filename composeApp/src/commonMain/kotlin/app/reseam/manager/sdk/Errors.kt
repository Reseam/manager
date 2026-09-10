package app.reseam.manager.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Problem {
    @Serializable @SerialName("bundle_too_old") data class BundleTooOld(val bundle: String, val built: String, val running: String) : Problem
    @Serializable @SerialName("engine_too_old") data class EngineTooOld(val bundle: String, val built: String, val running: String) : Problem
    @Serializable @SerialName("untrusted_bundle") data class UntrustedBundle(val path: String, val publicKey: String) : Problem
    @Serializable @SerialName("unreadable_bundle") data class UnreadableBundle(val path: String) : Problem
    @Serializable @SerialName("unreadable_apk") data class UnreadableApk(val path: String) : Problem
    @Serializable @SerialName("patches_failed") data class PatchesFailed(val patches: List<String>) : Problem
    @Serializable @SerialName("other") data object Other : Problem
}

@Serializable
internal data class SdkError(val problem: Problem, val message: String)

/** An engine failure with what the host can do about it. [message] is the engine's own text. */
class ReseamException(val problem: Problem, message: String) : Exception(message)

internal fun reseamException(raw: String): ReseamException =
    runCatching { WireJson.decodeFromString<SdkError>(raw) }
        .map { ReseamException(it.problem, it.message) }
        .getOrElse { ReseamException(Problem.Other, raw) }
