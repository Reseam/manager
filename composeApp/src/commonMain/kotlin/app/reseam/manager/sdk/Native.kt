package app.reseam.manager.sdk

internal expect object ReseamNative {
    fun inspect(requestJson: String): String
    fun patch(requestJson: String, onEvent: (String) -> Unit): String
}
