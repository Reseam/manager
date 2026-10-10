package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.SavedApk
import app.reseam.manager.data.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedApkRow(val apk: SavedApk, val inUse: Boolean)

class SavedApksViewModel(private val graph: AppGraph) : ViewModel() {
    val settings: StateFlow<Settings> = graph.settings.settings

    val rows: StateFlow<List<SavedApkRow>> = combine(graph.savedApks.apks, graph.patchedApps.apps) { apks, patched ->
        val sources = patched.mapNotNull { it.sourceApkPath }.toSet()
        apks.sortedByDescending { it.savedAtEpochMs }.map { SavedApkRow(it, it.path in sources) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setKeepDownloads(keep: Boolean) {
        viewModelScope.launch { graph.notices.attempt { graph.settings.update { it.copy(keepDownloads = keep) } } }
    }

    fun remove(apk: SavedApk) {
        viewModelScope.launch { graph.notices.attempt { graph.savedApks.remove(apk.id) } }
    }
}
