package app.reseam.manager.platform

interface BackgroundRun {
    suspend fun <T> hold(block: suspend () -> T): T

    fun finished(title: String, text: String)
}
