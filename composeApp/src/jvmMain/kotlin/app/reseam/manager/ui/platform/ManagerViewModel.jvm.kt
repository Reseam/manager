package app.reseam.manager.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.reseam.manager.di.createDesktopManagerViewModel
import app.reseam.manager.ui.viewmodel.ManagerViewModel

@Composable
fun rememberDesktopManagerViewModel(): ManagerViewModel {
    val scope = rememberCoroutineScope()
    return rememberManagerViewModel(scope, ::createDesktopManagerViewModel)
}
