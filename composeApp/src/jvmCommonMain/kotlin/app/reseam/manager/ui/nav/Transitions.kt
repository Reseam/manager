package app.reseam.manager.ui.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay
import app.reseam.manager.ui.theme.Motion

private const val FadeThroughShare = 35

fun sharedAxis(slide: Int): ContentTransform {
    val enter = tween<Float>(Motion.medium2 - Motion.medium2 * FadeThroughShare / 100, delayMillis = Motion.medium2 * FadeThroughShare / 100, easing = Motion.emphasizedDecelerate)
    val exit = tween<Float>(Motion.medium2 * FadeThroughShare / 100, easing = Motion.emphasizedAccelerate)
    return (slideInHorizontally(tween(Motion.medium2, easing = Motion.emphasized)) { slide } + fadeIn(enter)) togetherWith
        (slideOutHorizontally(tween(Motion.medium2, easing = Motion.emphasized)) { -slide } + fadeOut(exit))
}

private const val BackScale = 0.9f
private const val BackFadeDelay = Motion.medium2 * 2 / 3

val PredictiveBack: ContentTransform = EnterTransition.None togetherWith
    (scaleOut(tween(Motion.medium2, easing = LinearEasing), targetScale = BackScale) + fadeOut(tween(Motion.medium2 - BackFadeDelay, delayMillis = BackFadeDelay, easing = LinearEasing)))

private val Hold = EnterTransition.None togetherWith fadeOut(tween(ContainerMillis), targetAlpha = 1f)

val ContainerTransition: Map<String, Any> =
    NavDisplay.transitionSpec { Hold } + NavDisplay.popTransitionSpec { PredictiveBack } + NavDisplay.predictivePopTransitionSpec { PredictiveBack }
