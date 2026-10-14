package app.reseam.manager.platform

import android.content.ClipData
import android.os.Build
import androidx.compose.ui.platform.ClipEntry

internal actual fun textClipEntry(label: String, text: String): ClipEntry = ClipEntry(ClipData.newPlainText(label, text))

internal actual val systemConfirmsCopy: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
