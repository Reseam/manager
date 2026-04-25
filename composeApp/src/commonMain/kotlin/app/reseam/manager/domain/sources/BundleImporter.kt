package app.reseam.manager.domain.sources

import app.reseam.manager.ui.model.BundleSummary

interface BundleImporter {
    suspend fun importFromUrl(url: String): BundleSummary
    suspend fun importFromFile(path: String): BundleSummary
}
