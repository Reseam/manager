package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.theme.ReseamTheme

internal val PatchFlowSteps: List<String> = listOf("Inputs", "Patches", "Run")

@Composable
fun RsStepLabel(
    step: Int,
    modifier: Modifier = Modifier,
    color: Color = ReseamTheme.colors.primary,
) {
    Text(
        text = "STEP " + step.toString().padStart(2, '0'),
        style = ReseamTheme.typography.label,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun RsStepper(
    current: Int,
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val motion = ReseamTheme.motion
            steps.forEachIndexed { i, _ ->
                val done = i < current
                val active = i == current
                val targetCircleColor = if (done || active) colors.primary else colors.accent
                val targetLabelColor = if (done || active) colors.primaryForeground else colors.mutedForeground
                val circleColor by animateColorAsState(
                    targetValue = targetCircleColor,
                    animationSpec = tween(motion.durationBase, easing = motion.easeOut),
                    label = "rs-step-circle",
                )
                val labelColor by animateColorAsState(
                    targetValue = targetLabelColor,
                    animationSpec = tween(motion.durationBase, easing = motion.easeOut),
                    label = "rs-step-label",
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(circleColor, CircleShape)
                        .let {
                            if (active) {
                                it.border(2.dp, colors.primaryHairline, CircleShape)
                            } else it
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(
                            imageVector = ReseamIcons.Check,
                            contentDescription = null,
                            tint = labelColor,
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                        )
                    } else {
                        Text(
                            text = (i + 1).toString(),
                            style = ReseamTheme.typography.captionSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = labelColor,
                        )
                    }
                }
                if (i < steps.size - 1) {
                    val railColor by animateColorAsState(
                        targetValue = if (i < current) colors.primary else colors.accent,
                        animationSpec = tween(motion.durationSlow / 2, easing = motion.easeOut),
                        label = "rs-step-rail",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(railColor, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEachIndexed { i, name ->
                val align = when (i) {
                    0 -> TextAlign.Start
                    steps.size - 1 -> TextAlign.End
                    else -> TextAlign.Center
                }
                Text(
                    text = name,
                    style = ReseamTheme.typography.captionSmall.copy(
                        fontWeight = if (i == current) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = if (i == current) colors.foreground else colors.mutedForeground,
                    textAlign = align,
                    modifier = Modifier.widthIn(min = 80.dp).width(80.dp),
                )
            }
        }
    }
}
