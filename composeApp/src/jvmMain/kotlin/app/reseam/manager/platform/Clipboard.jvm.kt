package app.reseam.manager.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual fun textClipEntry(text: String): ClipEntry = ClipEntry(StringSelection(text))
