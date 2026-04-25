package app.reseam.manager.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.reseam.manager.data.platform.DesktopFilePicker
import app.reseam.manager.domain.sources.FilePicker

@Composable
actual fun rememberPlatformFilePicker(): FilePicker =
    remember { DesktopFilePicker() }
