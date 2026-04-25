package app.reseam.manager.domain.repository

import app.reseam.manager.ui.model.SettingsState

interface SettingsStore {
    suspend fun load(): SettingsState
    suspend fun save(settings: SettingsState)
}
