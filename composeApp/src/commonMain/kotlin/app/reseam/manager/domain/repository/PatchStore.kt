package app.reseam.manager.domain.repository

import app.reseam.manager.patcher.PatchMetadata

interface PatchStore {
    suspend fun listForBundle(bundleId: String): List<PatchMetadata>
    suspend fun listByPackage(packageName: String): List<PatchMetadata>
    suspend fun compatibleCountByPackage(): Map<String, Int>
    suspend fun replaceForBundle(bundleId: String, patches: List<PatchMetadata>)
    suspend fun deleteForBundle(bundleId: String)
}
