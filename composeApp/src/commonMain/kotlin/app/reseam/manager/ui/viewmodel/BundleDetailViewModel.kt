package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.ui.model.BundleDetailContent
import app.reseam.manager.ui.model.navigation.ManagerRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class BundleDetailViewModel internal constructor(
    private val store: ManagerStateStore,
    private val bundles: BundleStore,
    private val patchStore: PatchStore,
    private val scope: CoroutineScope,
) {
    fun open(bundleId: String) {
        scope.launch {
            val bundle = bundles.list().firstOrNull { it.id == bundleId } ?: return@launch
            val patches = patchStore.listForBundle(bundleId)
            store.update {
                it.copy(
                    navigation = it.navigation.push(ManagerRoute.BundleDetail(bundleId)),
                    bundles = it.bundles.copy(detail = BundleDetailContent(bundle, patches)),
                    error = null,
                )
            }
        }
    }

    fun close() {
        store.navigate(clearError = false) { it.pop() }
        store.updateBundles { it.copy(detail = null) }
    }
}
