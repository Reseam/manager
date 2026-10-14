package app.reseam.manager.ui.nav

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.reseam.manager.ui.theme.Motion

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

object SharedKeys {
    fun app(packageName: String) = "app:$packageName"
}

internal const val ContainerMillis = Motion.long2
private const val FadeOutMillis = ContainerMillis * 3 / 10

enum class ContainerEnd { Opener, Opened }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedContainer(key: String, shape: Shape, end: ContainerEnd): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedContentScope.current
    val opening = if (end == ContainerEnd.Opener) EnterExitState.PostExit else EnterExitState.Visible
    return with(shared) {
        val config = remember(visibility, opening) {
            object : SharedTransitionScope.SharedContentConfig {
                override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                    get() = visibility.transition.targetState == opening
            }
        }
        sharedBounds(
            sharedContentState = rememberSharedContentState(key, config),
            animatedVisibilityScope = visibility,
            enter = fadeIn(tween(ContainerMillis - FadeOutMillis, delayMillis = FadeOutMillis, easing = LinearEasing)),
            exit = fadeOut(tween(FadeOutMillis, easing = LinearEasing)),
            boundsTransform = { _, _ -> tween(ContainerMillis, easing = Motion.emphasized) },
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}
