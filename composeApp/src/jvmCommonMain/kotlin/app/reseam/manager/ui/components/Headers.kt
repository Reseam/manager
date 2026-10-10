package app.reseam.manager.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import app.reseam.manager.ui.theme.Borders
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Motion
import app.reseam.manager.ui.theme.Opacity
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.theme.extendedColors

@Composable
fun AppHeader(app: AppLook, supporting: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    if (compact) {
        Row(
            modifier = modifier.fillMaxWidth().padding(start = Layout.margin, end = Layout.margin, top = Space.sm, bottom = Space.xxl),
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app, Sizes.appIconLg)
            ItemText(app.name, supporting, Modifier.weight(1f), titleStyle = MaterialTheme.typography.titleLarge)
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(start = Layout.margin, end = Layout.margin, top = Space.sm, bottom = Space.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            AppIcon(app, Sizes.appIconXl)
            ItemText(app.name, supporting, titleStyle = MaterialTheme.typography.headlineSmall, centered = true)
        }
    }
}

sealed interface RunPhase {
    data class Working(val progress: Float?) : RunPhase
    data object Succeeded : RunPhase
    data object Failed : RunPhase
}

@Composable
fun RunHeader(app: AppLook, phase: RunPhase, title: String, supporting: String, modifier: Modifier = Modifier, below: @Composable () -> Unit = {}) {
    Column(
        modifier = modifier.fillMaxWidth().padding(start = Layout.margin, end = Layout.margin, top = Space.xxl, bottom = Space.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (phase is RunPhase.Working) Space.xxl else Space.xxxl),
    ) {
        when (phase) {
            is RunPhase.Working -> ProgressRing(app, phase.progress)
            RunPhase.Succeeded -> ResultRing(app, failed = false)
            RunPhase.Failed -> ResultRing(app, failed = true)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            ItemText(title, supporting, titleStyle = MaterialTheme.typography.headlineSmall, centered = true)
            below()
        }
    }
}

@Composable
private fun ProgressRing(app: AppLook, progress: Float?) {
    val colors = MaterialTheme.colorScheme
    val shown by animateFloatAsState(progress ?: 0f, tween(Motion.medium2, easing = Motion.emphasized))
    val rotation by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)))
    Box(Modifier.size(Sizes.progressPatching), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Borders.indicator.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(colors.secondaryContainer, 0f, 360f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
            val (start, sweep) = if (progress == null) rotation - 90f to 90f else -90f to 360f * shown
            drawArc(colors.primary, start, sweep, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        AppIcon(app, Sizes.appIconPatching)
    }
}

@Composable
private fun ResultRing(app: AppLook, failed: Boolean) {
    val colors = MaterialTheme.colorScheme
    val halo = if (failed) MaterialTheme.extendedColors.haloFailure else MaterialTheme.extendedColors.haloSuccess
    val ring = if (failed) colors.error else colors.primary
    Box(
        modifier = Modifier.size(Sizes.progressResult).drawBehind {
            val radius = size.minDimension * 0.6f
            drawCircle(Brush.radialGradient(0.7f to halo.copy(alpha = Opacity.halo), 1f to Color.Transparent, center = center, radius = radius), radius)
            drawCircle(colors.surface)
            val stroke = Borders.ring.toPx()
            drawCircle(ring, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
        },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(app, Sizes.appIconHero)
    }
}
