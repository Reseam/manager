package app.reseam.manager.domain.repository

import app.reseam.manager.ui.model.PatchedAppSummary

interface PatchedAppStore {
    suspend fun list(): List<PatchedAppSummary>
    suspend fun save(app: PatchedAppSummary)
}
