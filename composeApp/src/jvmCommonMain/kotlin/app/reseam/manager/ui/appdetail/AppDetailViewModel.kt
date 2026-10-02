package app.reseam.manager.ui.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.userMessage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppDetailViewModel(private val graph: AppGraph, private val packageName: String) : ViewModel() {
    val app: StateFlow<PatchedApp?> = graph.patchedApps.find(packageName).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val artifactActionLabel: String = graph.artifactAction.label

    val sourceAvailable: StateFlow<Boolean> = combine(app, graph.savedApks.apks) { app, _ -> app }
        .map { app -> app == null || withContext(Dispatchers.IO) { PlatformFile(app.sourceApkPath ?: app.apkPath).exists() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val installedState = MutableStateFlow<String?>(null)

    val installed: StateFlow<String?> = installedState.asStateFlow()

    fun openArtifact() {
        val app = app.value ?: return
        viewModelScope.launch { installedState.value = graph.deliverArtifact(PlatformFile(app.apkPath)) }
    }

    fun saveArtifact() {
        val app = app.value ?: return
        viewModelScope.launch { graph.saveArtifact(PlatformFile(app.apkPath)) }
    }

    fun openInstalled() {
        installed.value?.let(graph::openApp)
    }

    fun remove(onRemoved: () -> Unit) {
        viewModelScope.launch {
            runCatching { graph.patchedApps.remove(packageName) }
                .onSuccess { onRemoved() }
                .onFailure { graph.notices.warn(it.userMessage()) }
        }
    }
}
