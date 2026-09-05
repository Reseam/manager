package app.reseam.manager.ui.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.userMessage
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppDetailViewModel(private val graph: AppGraph, private val packageName: String) : ViewModel() {
    val app: StateFlow<PatchedApp?> = graph.patchedApps.find(packageName).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val artifactActionLabel: String = graph.artifactAction.label

    fun openArtifact() {
        val app = app.value ?: return
        viewModelScope.launch {
            runCatching { graph.artifactAction.run(PlatformFile(app.apkPath)) }
                .onFailure { graph.notices.post(it.userMessage()) }
        }
    }

    fun remove(onRemoved: () -> Unit) {
        viewModelScope.launch {
            runCatching { graph.patchedApps.remove(packageName) }
                .onSuccess { onRemoved() }
                .onFailure { graph.notices.post(it.userMessage()) }
        }
    }
}
