package app.reseam.manager.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.SavedApk
import app.reseam.manager.userMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class SavedApkRow(val apk: SavedApk, val inUse: Boolean)

class SavedApksViewModel(private val graph: AppGraph) : ViewModel() {
    val rows: StateFlow<List<SavedApkRow>?> = combine(graph.savedApks.apks, graph.patchedApps.apps) { apks, patched ->
        val sources = patched.mapNotNull { it.sourceApkPath }.toSet()
        apks.sortedByDescending { it.savedAtEpochMs }.map { SavedApkRow(it, it.path in sources) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(apk: SavedApk) {
        viewModelScope.launch {
            runCatching { graph.savedApks.remove(apk.id) }.onFailure { graph.notices.post(it.userMessage()) }
        }
    }
}

fun byteSize(bytes: Long): String {
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (bytes < 1024) "$bytes B" else String.format(Locale.ROOT, if (value < 10) "%.1f %s" else "%.0f %s", value, units[unit])
}
