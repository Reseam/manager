package app.reseam.manager.data.platform

import app.reseam.manager.FfiException
import app.reseam.manager.PatchEventSink
import app.reseam.manager.patcher.ApkMetadata
import app.reseam.manager.patcher.InspectRequest
import app.reseam.manager.patcher.InspectResponse
import app.reseam.manager.patcher.PatchOutcome
import app.reseam.manager.patcher.PatchRequest
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.patcher.ReseamJson
import app.reseam.manager.patcher.RunEvent
import app.reseam.manager.patcher.onSuccess
import app.reseam.manager.inspectApkJson
import app.reseam.manager.inspectJson
import app.reseam.manager.patchJson
import app.reseam.manager.ManagerAndroidHost
import kotlinx.serialization.encodeToString

class AndroidReseamBackend : ReseamBackend {
    override suspend fun inspectApk(
        apkPath: String,
        splitPaths: List<String>,
    ): ReseamCallResult<ApkMetadata> =
        callNative {
            inspectApkJson(apkPath, ReseamJson.codec.encodeToString(splitPaths))
        }.decode()

    override suspend fun inspect(request: InspectRequest): ReseamCallResult<InspectResponse> =
        callNative {
            inspectJson(ReseamJson.codec.encodeToString(request))
        }.decode()

    override suspend fun patch(
        request: PatchRequest,
        onEvent: (RunEvent) -> Unit,
    ): ReseamCallResult<PatchOutcome> =
        callNative {
            patchJson(
                ReseamJson.codec.encodeToString(request),
                object : PatchEventSink {
                    override fun onEvent(eventJson: String) {
                        ReseamJson.decode<RunEvent>(eventJson).onSuccess(onEvent)
                    }
                },
            )
        }.decode()

    fun installPatchClassLoader(classLoader: ClassLoader): ReseamCallResult<Unit> =
        callNative { ManagerAndroidHost.setClassLoader(classLoader) }

    private inline fun <T> callNative(block: () -> T): ReseamCallResult<T> =
        try {
            ReseamCallResult.Success(block())
        } catch (error: FfiException) {
            ReseamCallResult.Failure(error.message ?: "Native call failed")
        } catch (error: LinkageError) {
            ReseamCallResult.Failure(error.message ?: "Native link error: ${error::class.simpleName}")
        }

    private inline fun <reified T> ReseamCallResult<String>.decode(): ReseamCallResult<T> =
        when (this) {
            is ReseamCallResult.Success -> ReseamJson.decode(value)
            is ReseamCallResult.Failure -> this
        }
}
