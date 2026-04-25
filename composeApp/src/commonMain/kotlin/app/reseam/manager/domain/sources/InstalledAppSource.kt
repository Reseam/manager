package app.reseam.manager.domain.sources

import app.reseam.manager.ui.model.InstalledAppSummary

interface InstalledAppSource {
    suspend fun installedApps(): List<InstalledAppSummary>
}
