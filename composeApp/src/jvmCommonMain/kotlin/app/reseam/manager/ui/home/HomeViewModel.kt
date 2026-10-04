package app.reseam.manager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.BundleUpdate
import app.reseam.manager.data.ManagerUpdate
import app.reseam.manager.data.PatchedApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeState(
    val patchedApps: List<PatchedApp> = emptyList(),
    val hasBundles: Boolean = true,
    val syncing: Boolean = false,
    val bundleUpdate: BundleUpdate? = null,
    val update: ManagerUpdate? = null,
    val updateInstallable: Boolean = false,
)

class HomeViewModel(private val graph: AppGraph) : ViewModel() {
    val state: StateFlow<HomeState> = combine(graph.patchedApps.apps, graph.bundles.bundles, graph.bundles.syncing, graph.bundles.updating, graph.managerUpdates.update) { apps, bundles, syncing, bundleUpdate, update ->
        HomeState(
            patchedApps = apps.sortedByDescending { it.patchedAtEpochMs },
            hasBundles = bundles.isNotEmpty(),
            syncing = syncing,
            bundleUpdate = bundleUpdate,
            update = update,
            updateInstallable = graph.managerUpdates.installable,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun retrySync() = graph.syncBundles(force = true)

    fun installUpdate() = graph.managerUpdates.install()

    fun cancelUpdate() = graph.managerUpdates.cancel()
}
