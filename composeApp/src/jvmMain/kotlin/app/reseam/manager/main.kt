package app.reseam.manager

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.reseam.manager.ui.ReseamManagerApp
import app.reseam.manager.ui.platform.rememberDesktopManagerViewModel

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(480.dp, 820.dp))
    Window(
        onCloseRequest = ::exitApplication,
        title = "Reseam Manager",
        state = windowState,
    ) {
        ReseamManagerApp(rememberDesktopManagerViewModel())
    }
}
