package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Settings
import app.reseam.manager.data.SigningKeyInfo
import app.reseam.manager.userMessage
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val KeystoreExtensions = listOf("p12", "pfx", "jks", "keystore", "bks")

class SettingsViewModel(private val graph: AppGraph) : ViewModel() {
    val settings: StateFlow<Settings> = graph.settings.settings
    val signingKey: StateFlow<SigningKeyInfo?> = graph.signingKeys.info

    /** A picked keystore waiting for its password. */
    val pendingImport: StateFlow<PlatformFile?> get() = pendingImportState
    private val pendingImportState = MutableStateFlow<PlatformFile?>(null)

    fun setApiBaseUrl(url: String) {
        viewModelScope.launch {
            runCatching { graph.settings.update { it.copy(apiBaseUrl = url.trim().trimEnd('/')) } }
                .onSuccess { graph.syncOfficialBundle(force = true) }
                .onFailure { graph.notices.post(it.userMessage()) }
        }
    }

    fun setCheckUpdatesDaily(enabled: Boolean) = update { it.copy(checkUpdatesDaily = enabled) }

    fun setAllowIncompatiblePatches(enabled: Boolean) = update { it.copy(allowIncompatiblePatches = enabled) }

    fun exportSigningKey(password: String) = attempt {
        val file = FileKit.openFileSaver(suggestedName = "reseam-signing", defaultExtension = "p12") ?: return@attempt
        graph.signingKeys.export(file, password.toCharArray())
        graph.notices.post("Keystore saved as ${file.name}")
    }

    fun pickKeystore() = attempt {
        pendingImportState.value = FileKit.openFilePicker(type = FileKitType.File(KeystoreExtensions))
    }

    fun importSigningKey(password: String) = attempt {
        val file = pendingImportState.value ?: return@attempt
        try {
            graph.signingKeys.import(file, password.toCharArray())
            graph.notices.post("Signing key imported")
        } finally {
            pendingImportState.value = null
        }
    }

    fun cancelImport() {
        pendingImportState.value = null
    }

    fun resetSigningKey() = attempt {
        graph.signingKeys.reset()
        graph.notices.post("Signing key removed. A new one is created on the next patch.")
    }

    private fun update(transform: (Settings) -> Settings) = attempt { graph.settings.update(transform) }

    private fun attempt(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { graph.notices.post(it.userMessage()) }
        }
    }
}
