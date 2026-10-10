package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

@Composable
expect fun rememberApkPicker(onResult: (Result<PlatformFile?>) -> Unit): () -> Unit
