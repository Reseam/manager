package app.reseam.manager.domain.sources

import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.ui.model.BundleSummary

interface BundleImporter {
    /**
     * Fetches the official bundle index and downloads the latest stable release.
     *
     * If [currentVersion] equals the latest stable release version, returns null
     * to signal "already up to date". Pass null to force a download (e.g. first install).
     */
    suspend fun syncOfficial(apiBaseUrl: String, currentVersion: String?): BundleImportResult?

    suspend fun importFromUrl(url: String): BundleImportResult

    suspend fun importFromFile(path: String): BundleImportResult
}

data class BundleImportResult(
    val summary: BundleSummary,
    val patches: List<PatchMetadata>,
)
