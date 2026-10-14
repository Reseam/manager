package app.reseam.manager.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import app.reseam.manager.Notice
import app.reseam.manager.platform.systemConfirmsCopy
import app.reseam.manager.platform.textClipEntry
import kotlinx.coroutines.launch

@Composable
fun rememberClipboardCopy(): (label: String, text: suspend () -> String) -> Unit {
    val clipboard = LocalClipboard.current
    val notices = LocalNotices.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, notices, scope) {
        { label, text ->
            scope.launch {
                notices.attempt { clipboard.setClipEntry(textClipEntry(label, text())) } ?: return@launch
                if (!systemConfirmsCopy) notices.post(Notice.Copied)
            }
        }
    }
}
