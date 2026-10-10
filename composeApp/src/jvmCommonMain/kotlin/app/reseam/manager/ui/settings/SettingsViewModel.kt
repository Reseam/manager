package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.Notice
import app.reseam.manager.data.Settings
import app.reseam.manager.data.SigningKeyInfo
import app.reseam.manager.platform.ArtifactAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val graph: AppGraph) : ViewModel() {
    val settings: StateFlow<Settings> = graph.settings.settings
    val signingKey: StateFlow<SigningKeyInfo?> = graph.signingKeys.info

    val patchesVersion: StateFlow<String?> = graph.bundles.bundles
        .map { bundles -> bundles.firstOrNull { it.official }?.version }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Root managers such as KernelSU hide `su` from apps they have not granted, so root can't be detected up front. */
    val canMount: Boolean = graph.mounter != null
    val installs: Boolean = graph.actions.kind == ArtifactAction.Kind.Install

    private val askingRootState = MutableStateFlow(false)
    val askingRoot: StateFlow<Boolean> = askingRootState.asStateFlow()

    fun setAutoUpdateBundles(enabled: Boolean) = update { it.copy(autoUpdateBundles = enabled) }

    fun setAllowIncompatiblePatches(enabled: Boolean) = update { it.copy(allowIncompatiblePatches = enabled) }

    fun setUseSystemInstaller(enabled: Boolean) = update { it.copy(useSystemInstaller = enabled) }

    fun setMountWithRoot(enabled: Boolean) {
        if (!enabled) return update { it.copy(mountWithRoot = false) }
        viewModelScope.launch {
            askingRootState.value = true
            if (graph.mounter?.requestAccess() == true) graph.notices.attempt { graph.settings.update { it.copy(mountWithRoot = true) } }
            else graph.notices.post(Notice.RootRefused)
            askingRootState.value = false
        }
    }

    fun setApiBaseUrl(url: String) {
        viewModelScope.launch {
            graph.notices.attempt { graph.settings.update { it.copy(apiBaseUrl = url.trim().trimEnd('/')) } } ?: return@launch
            graph.bundleSyncer.sync(force = true)
        }
    }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { graph.notices.attempt { graph.settings.update(transform) } }
    }
}
