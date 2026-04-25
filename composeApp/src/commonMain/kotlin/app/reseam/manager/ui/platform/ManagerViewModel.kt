package app.reseam.manager.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.reseam.manager.ui.viewmodel.ManagerViewModel
import kotlinx.coroutines.CoroutineScope

@Composable
fun rememberManagerViewModel(
    scope: CoroutineScope,
    factory: (CoroutineScope) -> ManagerViewModel,
): ManagerViewModel =
    remember(scope) { factory(scope) }
