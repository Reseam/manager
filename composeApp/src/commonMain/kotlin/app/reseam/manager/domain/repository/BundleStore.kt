package app.reseam.manager.domain.repository

import app.reseam.manager.ui.model.BundleSummary

interface BundleStore {
    suspend fun list(): List<BundleSummary>
    suspend fun save(bundle: BundleSummary)
    suspend fun remove(bundleId: String)
}
