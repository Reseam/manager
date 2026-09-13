package app.reseam.manager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import app.reseam.manager.ui.theme.ReseamTheme

fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.97f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, ReseamTheme.motion.tweenFast(), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}
