package app.reseam.manager.domain.sources

import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.ui.model.BundleSummary

interface BundleImporter {
    suspend fun importOfficial(apiBaseUrl: String): BundleImportResult
    suspend fun importFromUrl(url: String): BundleImportResult
    suspend fun importFromFile(path: String): BundleImportResult
}

data class BundleImportResult(
    val summary: BundleSummary,
    val patches: List<PatchMetadata>,
)
