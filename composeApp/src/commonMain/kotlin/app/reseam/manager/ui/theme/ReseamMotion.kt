package app.reseam.manager.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable

@Immutable
data class ReseamMotion(
    val easeOut: Easing,
    val easeInOut: Easing,
    val easeSpring: Easing,
    val durationFast: Int,
    val durationBase: Int,
    val durationSlow: Int,
)

val ReseamDefaultMotion = ReseamMotion(
    easeOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
    easeInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f),
    easeSpring = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f),
    durationFast = 180,
    durationBase = 260,
    durationSlow = 600,
)
