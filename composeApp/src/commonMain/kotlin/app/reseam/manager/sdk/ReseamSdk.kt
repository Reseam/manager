package app.reseam.manager.sdk

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

object ReseamSdk {
    suspend fun inspect(request: InspectRequest): InspectResponse = withContext(Dispatchers.IO) {
        WireJson.decodeFromString(ReseamNative.inspect(WireJson.encodeToString(request)))
    }

    /** [onEvent] is invoked on the native thread while the engine runs. */
    suspend fun patch(request: PatchRequest, onEvent: (RunEvent) -> Unit): PatchOutcome = withContext(Dispatchers.IO) {
        val json = ReseamNative.patch(WireJson.encodeToString(request)) { onEvent(WireJson.decodeFromString(it)) }
        WireJson.decodeFromString(json)
    }
}
