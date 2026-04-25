package app.reseam.manager.patcher

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface ReseamBackend {
    suspend fun inspectApk(apkPath: String, splitPaths: List<String> = emptyList()): ReseamCallResult<ApkMetadata>

    suspend fun inspect(request: InspectRequest): ReseamCallResult<InspectResponse>

    suspend fun patch(
        request: PatchRequest,
        onEvent: (RunEvent) -> Unit = {},
    ): ReseamCallResult<PatchOutcome>
}

sealed interface ReseamCallResult<out T> {
    data class Success<T>(val value: T) : ReseamCallResult<T>

    data class Failure(val message: String) : ReseamCallResult<Nothing>
}

inline fun <T> ReseamCallResult<T>.onSuccess(block: (T) -> Unit): ReseamCallResult<T> {
    if (this is ReseamCallResult.Success) {
        block(value)
    }
    return this
}

inline fun <T> ReseamCallResult<T>.onFailure(block: (String) -> Unit): ReseamCallResult<T> {
    if (this is ReseamCallResult.Failure) {
        block(message)
    }
    return this
}

object ReseamJson {
    val codec = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        classDiscriminator = "type"
    }

    inline fun <reified T> decode(value: String): ReseamCallResult<T> =
        try {
            ReseamCallResult.Success(codec.decodeFromString<T>(value))
        } catch (error: SerializationException) {
            ReseamCallResult.Failure(error.message ?: "Invalid JSON response")
        } catch (error: IllegalArgumentException) {
            ReseamCallResult.Failure(error.message ?: "Invalid JSON response")
        }
}
