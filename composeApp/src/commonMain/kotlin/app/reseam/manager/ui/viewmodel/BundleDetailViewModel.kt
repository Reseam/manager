package app.reseam.manager.ui.viewmodel

import androidx.compose.runtime.Stable
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.ui.model.AppView
import app.reseam.manager.ui.model.BundleDetailContent
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
                    backStack = it.backStack + AppView.BundleDetail(bundleId),
                    bundles = it.bundles.copy(detail = BundleDetailContent(bundle, patches)),
                    error = null,
                )
            }
        }
    }

    fun close() {
        store.update {
            val view = it.view
            val nextStack = if (view is AppView.BundleDetail && it.canGoBack) it.backStack.dropLast(1) else it.backStack
            it.copy(backStack = nextStack, bundles = it.bundles.copy(detail = null))
        }
    }
}
