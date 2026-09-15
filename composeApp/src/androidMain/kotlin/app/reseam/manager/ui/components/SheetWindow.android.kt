package app.reseam.manager.ui.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
actual fun SheetWindow(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setWindowAnimations(0)
            @Suppress("DEPRECATION")
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        content()
    }
}
