package app.reseam.manager.ui.nav

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import app.reseam.manager.ui.theme.Radius

val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

object SharedKeys {
    fun app(packageName: String) = "app:$packageName"
    fun appIcon(packageName: String) = "app-icon:$packageName"
    fun appName(packageName: String) = "app-name:$packageName"
    fun appSupport(packageName: String) = "app-support:$packageName"
}

enum class ContainerEnd { Opener, Opened }

/** Motion specs for the shared transitions, after the Jetsnack shared element sample. */
fun <T> spatialExpressiveSpring() = spring<T>(dampingRatio = 0.8f, stiffness = 380f)

fun <T> nonSpatialExpressiveSpring() = spring<T>(dampingRatio = 1f, stiffness = 1600f)

private val containerBoundsTransform = BoundsTransform { _, _ -> spatialExpressiveSpring() }

/** Corner radius of the pill-shaped home cards the containers morph out of. */
private val MorphCorner = 40.dp

/** Morphs a home card into a full screen, e.g. the app card into the app page. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedContainer(key: String, shape: Shape, end: ContainerEnd): Modifier =
    shared(key, end) { state, visibility ->
        // Round the corners while flying out of a card, settling on the screen radius, like the
        // Jetsnack detail screen.
        val corner by visibility.transition.animateDp(label = "containerCorner") { state ->
            when (state) {
                EnterExitState.Visible -> Radius.xl
                EnterExitState.PreEnter, EnterExitState.PostExit -> MorphCorner
            }
        }
        sharedBounds(
            sharedContentState = state,
            animatedVisibilityScope = visibility,
            enter = fadeIn(nonSpatialExpressiveSpring()),
            exit = fadeOut(nonSpatialExpressiveSpring()),
            boundsTransform = containerBoundsTransform,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
            clipInOverlayDuringTransition = OverlayClip(if (end == ContainerEnd.Opened) RoundedCornerShape(corner) else shape),
        )
    }

/** Flies the app icon between its spot in a list and the header of the app page. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedIcon(key: String, end: ContainerEnd): Modifier =
    shared(key, end) { state, visibility ->
        // sharedElement draws exactly one copy mid flight, so nothing can double up. That is safe
        // against flicker now because the icon cache gives both sides the same painter immediately.
        sharedElement(
            sharedContentState = state,
            animatedVisibilityScope = visibility,
            boundsTransform = containerBoundsTransform,
            zIndexInOverlay = 1f,
            // The placeholder is drawn in the overlay, outside the normal modifier chain, so the
            // icon's own clip does not apply mid flight; clip it to the circle here instead.
            clipInOverlayDuringTransition = OverlayClip(CircleShape),
        )
    }

/** Quicker than [nonSpatialExpressiveSpring] so the outgoing and incoming text barely overlap. */
private fun <T> textSwapSpring() = spring<T>(dampingRatio = 1f, stiffness = 6000f)

/** Morphs a line of text between the list and the app page, e.g. the name or the version line. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedText(key: String, end: ContainerEnd): Modifier =
    shared(key, end) { state, visibility ->
        sharedBounds(
            sharedContentState = state,
            animatedVisibilityScope = visibility,
            enter = fadeIn(textSwapSpring()),
            exit = fadeOut(textSwapSpring()),
            boundsTransform = containerBoundsTransform,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.TopStart),
        )
    }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.shared(
    key: String,
    end: ContainerEnd,
    block: @Composable SharedTransitionScope.(SharedTransitionScope.SharedContentState, AnimatedVisibilityScope) -> Modifier,
): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedContentScope.current ?: return this
    return with(shared) {
        val config = remember(visibility, end) {
            object : SharedTransitionScope.SharedContentConfig {
                override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                    get() = when (end) {
                        // Only match while the opener is animating in either direction; while idle it sits
                        // next to the opened screen in wide two-pane layouts and must not pair up with it.
                        ContainerEnd.Opener -> EnterExitState.PostExit in setOf(visibility.transition.currentState, visibility.transition.targetState)
                        ContainerEnd.Opened -> true
                    }
            }
        }
        block(rememberSharedContentState(key, config), visibility)
    }
}
