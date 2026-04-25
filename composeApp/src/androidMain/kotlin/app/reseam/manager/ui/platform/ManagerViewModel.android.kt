package app.reseam.manager.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.reseam.manager.di.createAndroidManagerViewModel
import app.reseam.manager.ui.viewmodel.ManagerViewModel

@Composable
fun rememberAndroidManagerViewModel(): ManagerViewModel {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return rememberManagerViewModel(scope) {
        createAndroidManagerViewModel(context, it)
    }
}
