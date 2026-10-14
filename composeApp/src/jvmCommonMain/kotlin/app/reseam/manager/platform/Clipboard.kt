package app.reseam.manager.platform

import androidx.compose.ui.platform.ClipEntry

internal expect fun textClipEntry(label: String, text: String): ClipEntry

internal expect val systemConfirmsCopy: Boolean
