package app.reseam.manager.sdk

import app.reseam.sdk.PatchEventSink
import app.reseam.sdk.inspectJson
import app.reseam.sdk.patchJson

internal actual object ReseamNative {
    actual fun inspect(requestJson: String): String = inspectJson(requestJson)

    actual fun patch(requestJson: String, onEvent: (String) -> Unit): String =
        patchJson(requestJson, object : PatchEventSink {
            override fun onEvent(eventJson: String) = onEvent(eventJson)
        })
}
