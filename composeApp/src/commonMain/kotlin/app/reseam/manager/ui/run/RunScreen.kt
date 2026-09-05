package app.reseam.manager.ui.run

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.sdk.PatchStatus
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.LogDrawer
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RunScreen(
    viewModel: RunViewModel,
    target: PatchTarget,
    queue: List<String>,
    artifactActionLabel: String,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val finished = state.phase != RunPhase.Running
    Screen(
        title = when (state.phase) {
            RunPhase.Running -> "Patching"
            RunPhase.Finished -> if (state.failed.isEmpty()) "Ready" else "Ready, with warnings"
            RunPhase.Failed -> "Patching failed"
        },
        onBack = if (finished) onDone else null,
        header = { Stepper(current = 2) },
        bottomBar = if (!finished) null else {
            {
                BottomBar {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.phase == RunPhase.Finished) {
                            Button(onClick = viewModel::openArtifact, size = ButtonSize.Large, fullWidth = true) {
                                Icon(Icons.Download, null, modifier = Modifier.size(18.dp))
                                Text(artifactActionLabel)
                            }
                        }
                        Button(onClick = onDone, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Ghost) { Text("Done") }
                    }
                }
            }
        },
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (state.phase) {
                    RunPhase.Running -> Progress(state, target, queue)
                    RunPhase.Finished -> Result(state, target, queue)
                    RunPhase.Failed -> Failure(state, target)
                }
                Queue(queue, state, boxed = finished)
                LogDrawer(state.log)
            }
        }
    }
}

@Composable
private fun Progress(state: RunState, target: PatchTarget, queue: List<String>) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val done = queue.count { it in state.statuses }
    val fraction by animateFloatAsState(
        targetValue = if (queue.isEmpty()) 0f else (done + if (state.current != null) 0.5f else 0f) / queue.size,
        animationSpec = motion.tweenSlow(),
        label = "progress",
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(target.name, target.packageName, size = 52.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Patching", style = ReseamTheme.typography.label, color = colors.primary)
                Text(target.name, style = ReseamTheme.typography.title, color = colors.foreground)
            }
            Text("$done / ${queue.size}", style = ReseamTheme.typography.title, color = colors.foreground)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.muted)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(6.dp).background(Brush.horizontalGradient(listOf(colors.primary, colors.primaryBright))))
        }
        Card(borderColor = colors.primaryHairline, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Now applying", style = ReseamTheme.typography.label, color = colors.mutedForeground)
                    AnimatedContent(
                        targetState = state.current ?: "Preparing the app",
                        transitionSpec = {
                            (slideInVertically(motion.tweenBase()) { it / 3 } + fadeIn(motion.tweenBase()))
                                .togetherWith(slideOutVertically(motion.tweenFast()) { -it / 3 } + fadeOut(motion.tweenFast()))
                        },
                        label = "current-patch",
                    ) { Text(it, style = ReseamTheme.typography.titleSmall, color = colors.foreground) }
                }
                Icon(Icons.Sparkles, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Result(state: RunState, target: PatchTarget, queue: List<String>) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val warned = state.failed.isNotEmpty()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(if (shown) 1f else 0.6f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "badge")
    val alpha by animateFloatAsState(if (shown) 1f else 0f, motion.tweenBase(), label = "badge-alpha")
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .background(if (warned) colors.warningSoft else colors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (warned) Icons.TriangleAlert else Icons.Check,
                contentDescription = null,
                tint = if (warned) colors.warningForeground else colors.onPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${target.name} is patched", style = ReseamTheme.typography.headline, color = colors.foreground, textAlign = TextAlign.Center)
            Text(
                text = buildString {
                    append("${state.applied} of ${queue.size} patches applied")
                    if (warned) append(", ${state.failed.size} failed")
                    state.durationMs?.let { append(" in ${it / 1000}s") }
                },
                style = ReseamTheme.typography.bodySmall,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }
        if (state.output != null) {
            Card(modifier = Modifier.fillMaxWidth(), borderColor = colors.borderStrong, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppIcon(target.name, target.packageName)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(state.output.substringAfterLast('/'), style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
                        Text("signed apk", style = ReseamTheme.typography.monoSmall, color = colors.mutedForeground)
                    }
                }
            }
        }
        if (warned) {
            Banner("${state.failed.joinToString(", ")} failed to apply. Copy the log and share it with the patch author.")
        }
    }
}

@Composable
private fun Failure(state: RunState, target: PatchTarget) {
    val colors = ReseamTheme.colors
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(72.dp).background(colors.warningSoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.TriangleAlert, null, tint = colors.warningForeground, modifier = Modifier.size(34.dp))
        }
        Text("${target.name} was not patched", style = ReseamTheme.typography.headline, color = colors.foreground, textAlign = TextAlign.Center)
        Banner(state.error ?: "The engine stopped before producing an APK.")
    }
}

@Composable
private fun Queue(queue: List<String>, state: RunState, boxed: Boolean) {
    val colors = ReseamTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(if (boxed) 4.dp else 0.dp)) {
        SectionLabel("Patches", modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        queue.forEach { id ->
            val status = state.statuses[id]
            val active = state.current == id && status == null
            Card(
                modifier = Modifier.fillMaxWidth(),
                background = when {
                    boxed -> colors.surfaceSunken
                    active -> colors.surfaceElevated
                    else -> colors.background
                },
                borderColor = if (boxed) colors.divider else colors.background,
                shape = ReseamTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusDot(status, active)
                    Text(
                        text = id,
                        style = ReseamTheme.typography.bodySmall,
                        color = if (status != null || active) colors.foreground else colors.mutedForeground,
                        modifier = Modifier.weight(1f),
                    )
                    when (status) {
                        is PatchStatus.Failed -> Chip("Failed", variant = ChipVariant.Warning)
                        is PatchStatus.Skipped -> Chip("Skipped")
                        else -> Unit
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun StatusDot(status: PatchStatus?, active: Boolean) {
    val colors = ReseamTheme.colors
    val base = Modifier.size(18.dp).clip(CircleShape)
    when {
        status is PatchStatus.Applied -> Box(base.background(colors.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Check, null, tint = colors.onPrimary, modifier = Modifier.size(13.dp))
        }
        status is PatchStatus.Failed -> Box(base.background(colors.warningSoft).border(1.dp, colors.warningHairline, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.TriangleAlert, null, tint = colors.warningForeground, modifier = Modifier.size(12.dp))
        }
        status is PatchStatus.Skipped -> Box(base.background(colors.muted).border(1.dp, colors.divider, CircleShape))
        active -> Box(base.background(colors.primarySoft).border(1.dp, colors.primaryHairline, CircleShape))
        else -> Box(base.background(colors.mutedElevated))
    }
}
