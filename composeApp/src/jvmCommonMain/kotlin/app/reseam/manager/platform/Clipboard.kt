package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

internal expect fun textClipEntry(label: String, text: String): ClipEntry

@Composable
fun rememberClipboardCopy(): (label: String, text: String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) { { label, text -> scope.launch { clipboard.setClipEntry(textClipEntry(label, text)) } } }
}
