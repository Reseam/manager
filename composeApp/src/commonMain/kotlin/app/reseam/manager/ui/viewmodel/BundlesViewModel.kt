package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.PendingBundleTrust
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class BundlesViewModel internal constructor(
    private val store: ManagerStateStore,
    private val bundles: BundleStore,
    private val importer: BundleImporter?,
    private val scope: CoroutineScope,
) {
    fun importFromUrl(url: String) {
        importBundle { it.importFromUrl(url) }
    }

    fun importFromFile(path: String) {
        importBundle { it.importFromFile(path) }
    }

    fun decidePendingTrust(trust: Boolean) {
        val pending = store.state.bundles.pendingTrust ?: return
        scope.launch {
            if (trust) {
                val bundle = pending.bundle.copy(trusted = true)
                bundles.save(bundle)
                store.update {
                    it.copy(
                        bundles = it.bundles.copy(
                            installed = it.bundles.installed.filterNot { installed -> installed.id == bundle.id } + bundle,
                            pendingTrust = null,
                        ),
                    )
                }
            } else {
                store.update { it.copy(bundles = it.bundles.copy(pendingTrust = null)) }
            }
        }
    }

    fun remove(bundleId: String) {
        store.update {
            it.copy(bundles = it.bundles.copy(installed = it.bundles.installed.filter { bundle ->
                bundle.official || bundle.id != bundleId
            }))
        }
        scope.launch {
            bundles.remove(bundleId)
        }
    }

    private fun importBundle(load: suspend (BundleImporter) -> BundleSummary) {
        val bundleImporter = importer ?: return
        scope.launch {
            store.update { it.copy(bundles = it.bundles.copy(importing = true), error = null) }
            runCatching { load(bundleImporter) }
                .onSuccess { bundle ->
                    store.update {
                        it.copy(
                            bundles = it.bundles.copy(
                                importing = false,
                                pendingTrust = PendingBundleTrust(bundle),
                            ),
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
}
