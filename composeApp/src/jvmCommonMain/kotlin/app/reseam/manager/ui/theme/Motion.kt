package app.reseam.manager.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

@Immutable
data class ReseamMotion(
    val easeOut: Easing,
    val easeInOut: Easing,
    val fast: Int,
    val base: Int,
    val slow: Int,
) {
    fun <T> tweenFast(): TweenSpec<T> = tween(fast, easing = easeOut)
    fun <T> tweenBase(): TweenSpec<T> = tween(base, easing = easeOut)
    fun <T> tweenSlow(): TweenSpec<T> = tween(slow, easing = easeInOut)
}

val ReseamDefaultMotion = ReseamMotion(
    easeOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
    easeInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f),
    fast = 180,
    base = 260,
    slow = 600,
)
