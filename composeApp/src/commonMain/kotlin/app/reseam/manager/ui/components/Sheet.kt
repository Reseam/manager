package app.reseam.manager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RsBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ReseamTheme.colors
    val scrimInteraction = remember { MutableInteractionSource() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220, easing = ReseamTheme.motion.easeOut)),
        exit = fadeOut(animationSpec = tween(220, easing = ReseamTheme.motion.easeOut)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(
                    interactionSource = scrimInteraction,
                    indication = null,
                    onClick = onDismissRequest,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = tween(280, easing = ReseamTheme.motion.easeOut),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(220, easing = ReseamTheme.motion.easeOut),
                    targetOffsetY = { it },
                ),
            ) {
                val blockInteraction = remember { MutableInteractionSource() }
                Column(
                    modifier = modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .background(colors.card)
                        .border(
                            1.dp,
                            colors.borderStrong,
                            RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                        )
                        .clickable(
                            interactionSource = blockInteraction,
                            indication = null,
                            onClick = {},
                        )
                        .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .background(colors.borderStrong, RoundedCornerShape(50)),
                        )
                    }
                    content()
                }
            }
        }
    }
}
