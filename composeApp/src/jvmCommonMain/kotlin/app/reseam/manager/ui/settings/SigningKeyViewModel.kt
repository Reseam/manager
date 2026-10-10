package app.reseam.manager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.Notice
import app.reseam.manager.data.SigningKeyInfo
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

class SigningKeyViewModel(private val graph: AppGraph) : ViewModel() {
    val key: StateFlow<SigningKeyInfo?> = graph.signingKeys.info

    private val importingState = MutableStateFlow<PlatformFile?>(null)

    val importing: StateFlow<PlatformFile?> = importingState.asStateFlow()

    fun export(password: String) = work {
        val file = FileKit.openFileSaver(suggestedName = "reseam-signing", defaultExtension = "p12") ?: return@work
        graph.signingKeys.export(file, password.toCharArray())
        graph.notices.post(Notice.KeyExported(file.name))
    }

    fun pickKeystore() = work {
        importingState.value = FileKit.openFilePicker(type = FileKitType.File(KeystoreExtensions))
    }

    fun import(password: String) = work {
        val file = importingState.value ?: return@work
        importingState.value = null
        graph.signingKeys.import(file, password.toCharArray())
        graph.notices.post(Notice.KeyImported)
    }

    fun cancelImport() {
        importingState.value = null
    }

    fun reset() = work {
        graph.signingKeys.reset()
        graph.notices.post(Notice.KeyReset)
    }

    private fun work(block: suspend () -> Unit) {
        viewModelScope.launch { graph.notices.attempt(block) }
    }
}
