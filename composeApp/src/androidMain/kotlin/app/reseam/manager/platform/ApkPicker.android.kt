package app.reseam.manager.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.vinceglb.filekit.PlatformFile

private val ApkTypes = arrayOf("application/vnd.android.package-archive", "application/octet-stream")

@Composable
actual fun rememberApkPicker(onResult: (Result<PlatformFile?>) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onResult(runCatching { uri?.let(::PlatformFile) })
    }
    return remember(launcher) {
        {
            launcher.launch(ApkTypes)
        }
    }
}
