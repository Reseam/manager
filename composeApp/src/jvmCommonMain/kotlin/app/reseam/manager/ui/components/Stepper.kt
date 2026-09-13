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
import androidx.compose.foundation.layout.widthIn
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

/** Progress through the patch flow. Labels sit under the dots on phones and beside them where there is room. */
@Composable
fun Stepper(current: Int, modifier: Modifier = Modifier, steps: List<String> = PatchFlowSteps) {
    val layout = ReseamTheme.layout
    Box(modifier = modifier.fillMaxWidth().padding(horizontal = layout.pageMargin, vertical = 12.dp), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = layout.contentMaxWidth).fillMaxWidth()) {
            if (layout.compact) StackedStepper(current, steps) else InlineStepper(current, steps)
        }
    }
}

@Composable
private fun StackedStepper(current: Int, steps: List<String>) {
    val colors = ReseamTheme.colors
    // Each label sits in the same column as its dot. Laying the two out as separate
    // rows left the labels spread edge to edge and none of them under their dot.
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, name ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepDot(index, current)
                Text(
                    text = name,
                    style = ReseamTheme.typography.captionSmall.copy(fontWeight = if (index == current) FontWeight.Medium else FontWeight.Normal),
                    color = if (index == current) colors.foreground else colors.mutedForeground,
                )
            }
            if (index < steps.lastIndex) StepRail(index < current, Modifier.weight(1f).padding(top = StepDotSize / 2))
        }
    }
}

@Composable
private fun InlineStepper(current: Int, steps: List<String>) {
    val colors = ReseamTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { index, name ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepDot(index, current)
                Text(
                    text = name,
                    style = ReseamTheme.typography.captionMedium,
                    color = if (index == current) colors.foreground else colors.mutedForeground,
                )
            }
            if (index < steps.lastIndex) StepRail(index < current, Modifier.weight(1f))
        }
    }
}

private val StepDotSize = 26.dp

@Composable
private fun StepDot(index: Int, current: Int) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val reached = index <= current
    val circle by animateColorAsState(if (reached) colors.primary else colors.mutedElevated, motion.tweenBase(), label = "step")
    val text by animateColorAsState(if (reached) colors.onPrimary else colors.mutedForeground, motion.tweenBase(), label = "step-text")
    Box(
        modifier = Modifier
            .size(StepDotSize)
            .background(circle, CircleShape)
            .then(if (index == current) Modifier.border(2.dp, colors.primaryHairline, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (index < current) {
            Icon(Icons.Check, null, tint = text, modifier = Modifier.size(15.dp))
        } else {
            Text((index + 1).toString(), style = ReseamTheme.typography.chip, color = text)
        }
    }
}

@Composable
private fun StepRail(done: Boolean, modifier: Modifier) {
    val colors = ReseamTheme.colors
    val rail by animateColorAsState(if (done) colors.primary else colors.mutedElevated, ReseamTheme.motion.tweenBase(), label = "rail")
    Box(modifier.height(2.dp).background(rail, CircleShape))
}

/**
 * The one thing to do on this step, in the imperative. The stepper says where you
 * are; this says what to do, and it is the only line on the screen that asks for it.
 */
@Composable
fun StepInstruction(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = ReseamTheme.typography.display,
        color = ReseamTheme.colors.foreground,
        modifier = modifier.padding(top = 4.dp, bottom = 12.dp),
    )
}
