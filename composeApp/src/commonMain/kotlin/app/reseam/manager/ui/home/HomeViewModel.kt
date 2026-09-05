package app.reseam.manager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.OfficialReleaseInfo
import app.reseam.manager.data.PatchedApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeState(
    val patchedApps: List<PatchedApp> = emptyList(),
    val hasBundles: Boolean = true,
    val syncing: Boolean = false,
    val update: OfficialReleaseInfo? = null,
)

class HomeViewModel(graph: AppGraph) : ViewModel() {
    val state: StateFlow<HomeState> = combine(graph.patchedApps.apps, graph.bundles.bundles, graph.bundles.syncing, graph.managerUpdate) { apps, bundles, syncing, update ->
        HomeState(
            patchedApps = apps.sortedByDescending { it.patchedAtEpochMs },
            hasBundles = bundles.isNotEmpty(),
            syncing = syncing,
            update = update,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
