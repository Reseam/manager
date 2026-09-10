package app.reseam.manager.sdk

import app.reseam.sdk.FfiException
import app.reseam.sdk.PatchEventSink
import app.reseam.sdk.inspectJson
import app.reseam.sdk.patchJson

internal actual object ReseamNative {
    actual fun inspect(requestJson: String): String = engine { inspectJson(requestJson) }

    actual fun patch(requestJson: String, onEvent: (String) -> Unit): String = engine {
        patchJson(requestJson, object : PatchEventSink {
            override fun onEvent(eventJson: String) = onEvent(eventJson)
        })
    }

    private inline fun engine(call: () -> String): String =
        try {
            call()
        } catch (error: FfiException) {
            throw reseamException(error.message.orEmpty())
        }
}
