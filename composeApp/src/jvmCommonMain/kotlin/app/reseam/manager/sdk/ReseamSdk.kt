package app.reseam.manager.sdk

import app.reseam.sdk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReseamSdk {
    suspend fun inspect(request: InspectRequest): InspectResponse = withContext(Dispatchers.IO) {
        app.reseam.sdk.inspect(request)
    }

    /** [onEvent] runs synchronously on the native worker thread. */
    suspend fun patch(request: PatchRequest, onEvent: (RunEvent) -> Unit): PatchOutcome = withContext(Dispatchers.IO) {
        app.reseam.sdk.patch(request, onEvent)
    }
}
