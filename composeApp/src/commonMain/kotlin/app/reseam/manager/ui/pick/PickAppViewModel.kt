package app.reseam.manager.ui.pick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.platform.InstalledApp
import app.reseam.manager.platform.localCopy
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.userMessage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PickMode { Installed, File }

data class InstalledCandidate(val app: InstalledApp, val patchCount: Int)

data class PickAppState(
    val mode: PickMode,
    val query: String = "",
    val selected: PatchTarget? = null,
    val pickingFile: Boolean = false,
)

class PickAppViewModel(private val graph: AppGraph) : ViewModel() {
    val installedSupported: Boolean = graph.installedApps != null

    private val current = MutableStateFlow(PickAppState(mode = if (installedSupported) PickMode.Installed else PickMode.File))
    val state: StateFlow<PickAppState> = current.asStateFlow()

    /** Installed apps that at least one installed patch targets, most patches first. Null while loading. */
    val candidates: StateFlow<List<InstalledCandidate>?> = graph.bundles.bundles
        .map { bundles ->
            val counts = bundles.flatMap { it.patches }.flatMap { patch -> patch.compatibility.map { it.`package` } }.groupingBy { it }.eachCount()
            val apps = graph.installedApps?.query(counts.keys).orEmpty()
            apps.map { InstalledCandidate(it, counts.getValue(it.packageName)) }
                .sortedWith(compareByDescending<InstalledCandidate> { it.patchCount }.thenBy { it.app.name.lowercase() })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setMode(mode: PickMode) = current.update { it.copy(mode = mode, selected = null) }

    fun setQuery(query: String) = current.update { it.copy(query = query) }

    fun select(app: InstalledApp) = current.update {
        it.copy(selected = PatchTarget(app.name, app.packageName, app.versionName, app.apkPath, app.splitPaths))
    }

    fun pickFile() {
        if (current.value.pickingFile) return
        current.update { it.copy(pickingFile = true) }
        viewModelScope.launch {
            try {
                val picked = FileKit.openFilePicker(type = FileKitType.File(listOf("apk"))) ?: return@launch
                val local = picked.localCopy(graph.cacheDirectory / "apk")
                current.update { it.copy(selected = PatchTarget(picked.nameWithoutExtension, null, null, local.absolutePath())) }
            } catch (error: Exception) {
                graph.notices.post(error.userMessage())
            } finally {
                current.update { it.copy(pickingFile = false) }
            }
        }
    }
}

fun List<InstalledCandidate>.matching(query: String): List<InstalledCandidate> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return this
    return filter { it.app.name.lowercase().contains(needle) || it.app.packageName.contains(needle) }
}
