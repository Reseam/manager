package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.BundleImportResult
import app.reseam.manager.domain.sources.OfficialBundleId
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.PendingBundleTrust
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class BundlesViewModel internal constructor(
    private val store: ManagerStateStore,
    private val bundles: BundleStore,
    private val patchStore: PatchStore,
    private val importer: BundleImporter?,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    fun importFromUrl(url: String) {
        importBundle { it.importFromUrl(url) }
    }

    fun importFromFile(path: String) {
        importBundle { it.importFromFile(path) }
    }

    fun refreshOfficial() {
        val bundleImporter = importer ?: return
        scope.launch {
            store.updateBundles(clearError = true) { it.copy(importing = true) }
            val apiBaseUrl = settings.load().apiBaseUrl
            val current = bundles.list().firstOrNull { it.id == OfficialBundleId }
            runCatching { bundleImporter.syncOfficial(apiBaseUrl, current?.version) }
                .onSuccess { result -> applyOfficialResult(result) }
                .onFailure { error ->
                    store.update {
                        it.copy(
                            error = error.message ?: "Could not check for official bundle updates",
                            bundles = it.bundles.copy(importing = false),
                        )
                    }
                }
        }
    }

    fun decidePendingTrust(trust: Boolean) {
        val pending = store.state.bundles.pendingTrust ?: return
        scope.launch {
            if (trust) {
                val bundle = pending.bundle.copy(trusted = true)
                bundles.save(bundle)
                store.updateBundles {
                    it.copy(
                        installed = it.installed.filterNot { installed -> installed.id == bundle.id } + bundle,
                        pendingTrust = null,
                    )
                }
            } else {
                store.updateBundles { it.copy(pendingTrust = null) }
            }
        }
    }

    fun remove(bundleId: String) {
        if (bundleId == OfficialBundleId) return
        store.updateBundles {
            it.copy(installed = it.installed.filter { bundle ->
                bundle.official || bundle.id != bundleId
            })
        }
        scope.launch {
            bundles.remove(bundleId)
        }
    }

    private fun importBundle(load: suspend (BundleImporter) -> BundleImportResult) {
        val bundleImporter = importer ?: return
        scope.launch {
            store.updateBundles(clearError = true) { it.copy(importing = true) }
            runCatching { load(bundleImporter) }
                .onSuccess { result ->
                    bundles.save(result.summary)
                    patchStore.replaceForBundle(result.summary.id, result.patches)
                    store.updateBundles {
                        it.copy(
                            importing = false,
                            installed = mergeInstalled(it.installed, result.summary),
                            pendingTrust = if (result.summary.official) null else PendingBundleTrust(result.summary),
                        )
                    }
                }
                .onFailure { error ->
                    store.update {
                        it.copy(
                            error = error.message ?: "Bundle import failed",
                            bundles = it.bundles.copy(importing = false),
                        )
                    }
                }
        }
    }

    private suspend fun applyOfficialResult(result: BundleImportResult?) {
        if (result == null) {
            store.updateBundles { it.copy(importing = false) }
            return
        }
        bundles.save(result.summary)
        patchStore.replaceForBundle(result.summary.id, result.patches)
        store.updateBundles {
            it.copy(
                importing = false,
                installed = mergeInstalled(it.installed, result.summary),
            )
        }
    }

    private fun mergeInstalled(current: List<BundleSummary>, bundle: BundleSummary): List<BundleSummary> =
        current.filterNot { it.id == bundle.id } + bundle
}
