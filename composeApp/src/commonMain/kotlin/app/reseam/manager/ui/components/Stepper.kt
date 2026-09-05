package app.reseam.manager.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

val PatchFlowSteps = listOf("Pick app", "Patches", "Run")

@Composable
fun Stepper(current: Int, modifier: Modifier = Modifier, steps: List<String> = PatchFlowSteps) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.forEachIndexed { index, _ ->
                val reached = index <= current
                val circle by animateColorAsState(if (reached) colors.primary else colors.mutedElevated, motion.tweenBase(), label = "step")
                val text by animateColorAsState(if (reached) colors.onPrimary else colors.mutedForeground, motion.tweenBase(), label = "step-text")
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(circle, CircleShape)
                        .then(if (index == current) Modifier.border(2.dp, colors.primaryHairline, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index < current) {
                        Icon(Icons.Check, null, tint = text, modifier = Modifier.size(13.dp))
                    } else {
                        Text((index + 1).toString(), style = ReseamTheme.typography.captionSmall.copy(fontWeight = FontWeight.SemiBold), color = text)
                    }
                }
                if (index < steps.lastIndex) {
                    val rail by animateColorAsState(if (index < current) colors.primary else colors.mutedElevated, motion.tweenBase(), label = "rail")
                    Box(Modifier.weight(1f).height(2.dp).background(rail, CircleShape))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            steps.forEachIndexed { index, name ->
                Text(
                    text = name,
                    style = ReseamTheme.typography.captionSmall.copy(fontWeight = if (index == current) FontWeight.Medium else FontWeight.Normal),
                    color = if (index == current) colors.foreground else colors.mutedForeground,
                )
            }
        }
    }
}

@Composable
fun StepIntro(step: Int, title: String, body: String, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("STEP ${step.toString().padStart(2, '0')}", style = ReseamTheme.typography.label, color = colors.primary)
        Text(title, style = ReseamTheme.typography.display, color = colors.foreground)
        Text(body, style = ReseamTheme.typography.bodySmall, color = colors.mutedForeground)
    }
}
