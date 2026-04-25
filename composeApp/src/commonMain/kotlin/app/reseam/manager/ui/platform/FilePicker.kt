package app.reseam.manager.ui.platform

import androidx.compose.runtime.Composable
import app.reseam.manager.domain.sources.FilePicker

@Composable
expect fun rememberPlatformFilePicker(): FilePicker
