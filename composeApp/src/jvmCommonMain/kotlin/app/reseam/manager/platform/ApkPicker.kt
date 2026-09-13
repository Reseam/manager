package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

/** Lazily launches the platform APK picker and reports the chosen file, null when cancelled, or a failure. */
@Composable
expect fun rememberApkPicker(onResult: (Result<PlatformFile?>) -> Unit): () -> Unit
