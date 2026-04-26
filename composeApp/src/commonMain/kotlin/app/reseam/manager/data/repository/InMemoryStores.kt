package app.reseam.manager.data.repository

import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.model.SettingsState

class InMemoryPatchedAppStore(
    initial: List<PatchedAppSummary> = emptyList(),
) : PatchedAppStore {
    private val apps = initial.toMutableList()

    override suspend fun list(): List<PatchedAppSummary> = apps.toList()

    override suspend fun save(app: PatchedAppSummary) {
        apps.removeAll { it.id == app.id }
        apps += app
    }
}

class InMemoryBundleStore(
    initial: List<BundleSummary> = emptyList(),
) : BundleStore {
    private val bundles = initial.toMutableList()

    override suspend fun list(): List<BundleSummary> = bundles.toList()

    override suspend fun save(bundle: BundleSummary) {
        bundles.removeAll { it.id == bundle.id }
        bundles += bundle
    }

    override suspend fun remove(bundleId: String) {
        bundles.removeAll { !it.official && it.id == bundleId }
    }
}

class InMemorySettingsStore(
    initial: SettingsState = SettingsState(),
) : SettingsStore {
    private var settings = initial

    override suspend fun load(): SettingsState = settings

    override suspend fun save(settings: SettingsState) {
        this.settings = settings
    }
}

class InMemoryPatchStore : PatchStore {
    private val byBundle = mutableMapOf<String, List<PatchMetadata>>()

    override suspend fun listForBundle(bundleId: String): List<PatchMetadata> =
        byBundle[bundleId].orEmpty()

    override suspend fun listByPackage(packageName: String): List<PatchMetadata> =
        byBundle.values.flatten().filter { patch ->
            patch.compatibleWith.any { it.packageName == packageName }
        }

    override suspend fun compatibleCountByPackage(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        byBundle.values.flatten().forEach { patch ->
            patch.compatibleWith.forEach { compat ->
                counts[compat.packageName] = (counts[compat.packageName] ?: 0) + 1
            }
        }
        return counts
    }

    override suspend fun replaceForBundle(bundleId: String, patches: List<PatchMetadata>) {
        byBundle[bundleId] = patches
    }

    override suspend fun deleteForBundle(bundleId: String) {
        byBundle.remove(bundleId)
    }
}
