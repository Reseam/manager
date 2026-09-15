package app.reseam.manager.platform

import androidx.compose.ui.platform.ClipEntry

expect fun textClipEntry(label: String, text: String): ClipEntry
