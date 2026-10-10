package app.reseam.manager.ui.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.reseam.manager.ui.theme.Motion

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

object SharedKeys {
    fun app(packageName: String) = "app:$packageName"
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.shared(key: String, durationMillis: Int = Motion.long2): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedContentScope.current
    return with(shared) {
        sharedBounds(rememberSharedContentState(key), visibility, boundsTransform = { _, _ -> tween(durationMillis, easing = Motion.emphasized) })
    }
}
