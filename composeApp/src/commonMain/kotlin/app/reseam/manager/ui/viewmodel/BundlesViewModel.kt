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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Stable
class BundlesViewModel internal constructor(
    private val store: ManagerStateStore,
    private val bundles: BundleStore,
    private val patchStore: PatchStore,
    private val importer: BundleImporter?,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val mutationLock = Mutex()

    fun importFromUrl(url: String) = importBundle { it.importFromUrl(url) }

    fun importFromFile(path: String) = importBundle { it.importFromFile(path) }

    fun refreshOfficial() {
        val bundleImporter = importer ?: return
        launchMutation {
            store.update { it.copy(bundles = it.bundles.copy(importing = true), error = null) }
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
        launchMutation {
            if (trust) {
                val bundle = pending.bundle.copy(trusted = true)
                bundles.save(bundle)
                store.update {
                    it.copy(
                        bundles = it.bundles.copy(
                            installed = it.bundles.installed.filterNot { b -> b.id == bundle.id } + bundle,
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
        if (bundleId == OfficialBundleId) return
        store.update {
            it.copy(
                bundles = it.bundles.copy(
                    installed = it.bundles.installed.filter { b -> b.official || b.id != bundleId },
                ),
            )
        }
        launchMutation {
            bundles.remove(bundleId)
            patchStore.deleteForBundle(bundleId)
        }
    }

    private fun importBundle(load: suspend (BundleImporter) -> BundleImportResult) {
        val bundleImporter = importer ?: return
        launchMutation {
            store.update { it.copy(bundles = it.bundles.copy(importing = true), error = null) }
            runCatching { load(bundleImporter) }
                .onSuccess { result ->
                    bundles.save(result.summary)
                    patchStore.replaceForBundle(result.summary.id, result.patches)
                    store.update {
                        it.copy(
                            bundles = it.bundles.copy(
                                importing = false,
                                installed = mergeInstalled(it.bundles.installed, result.summary),
                                pendingTrust = if (result.summary.official) null else PendingBundleTrust(result.summary),
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

    private suspend fun applyOfficialResult(result: BundleImportResult?) {
        if (result == null) {
            store.update { it.copy(bundles = it.bundles.copy(importing = false)) }
            return
        }
        bundles.save(result.summary)
        patchStore.replaceForBundle(result.summary.id, result.patches)
        store.update {
            it.copy(
                bundles = it.bundles.copy(
                    importing = false,
                    installed = mergeInstalled(it.bundles.installed, result.summary),
                ),
            )
        }
    }

    private fun launchMutation(block: suspend () -> Unit) {
        scope.launch { mutationLock.withLock { block() } }
    }

    private fun mergeInstalled(current: List<BundleSummary>, bundle: BundleSummary): List<BundleSummary> =
        current.filterNot { it.id == bundle.id } + bundle
}
