package app.reseam.manager.ui.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay
import app.reseam.manager.ui.theme.Motion

private const val FadeThroughShare = 35

/** The standard slide for pages without shared elements: the pages pass left and right, overlapping. */
fun sharedAxis(slide: Int): ContentTransform {
    val enter = tween<Float>(Motion.medium2 - Motion.medium2 * FadeThroughShare / 100, delayMillis = Motion.medium2 * FadeThroughShare / 100, easing = Motion.emphasizedDecelerate)
    val exit = tween<Float>(Motion.medium2 * FadeThroughShare / 100, easing = Motion.emphasizedAccelerate)
    return (slideInHorizontally(tween(Motion.medium2, easing = Motion.emphasized)) { slide } + fadeIn(enter)) togetherWith
        (slideOutHorizontally(tween(Motion.medium2, easing = Motion.emphasized)) { -slide } + fadeOut(exit))
}

/** The app page morphs out of the home cards via shared elements, so it crossfades instead of sliding. */
private val Morph: ContentTransform = fadeIn(nonSpatialExpressiveSpring()) togetherWith fadeOut(nonSpatialExpressiveSpring())

/** Per-entry transitions for routes that morph out of the home cards, e.g. the app page. */
val ContainerTransition: Map<String, Any> =
    NavDisplay.transitionSpec { Morph } +
        NavDisplay.popTransitionSpec { Morph } +
        NavDisplay.predictivePopTransitionSpec { Morph }
