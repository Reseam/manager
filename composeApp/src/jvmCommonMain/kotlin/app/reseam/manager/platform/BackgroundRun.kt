package app.reseam.manager.platform

import kotlinx.coroutines.flow.Flow

interface BackgroundRun {
    suspend fun <T> hold(title: Flow<String>, block: suspend () -> T): T

    fun finished(title: String, text: String)
}
