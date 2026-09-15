package app.reseam.manager.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun textClipEntry(label: String, text: String): ClipEntry = ClipEntry(ClipData.newPlainText(label, text))
