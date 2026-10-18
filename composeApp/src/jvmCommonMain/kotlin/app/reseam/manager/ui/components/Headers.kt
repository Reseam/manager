package app.reseam.manager.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import app.reseam.manager.ui.theme.Borders
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.nav.ContainerEnd
import app.reseam.manager.ui.nav.SharedKeys
import app.reseam.manager.ui.nav.sharedIcon
import app.reseam.manager.ui.nav.sharedText
import app.reseam.manager.ui.theme.Motion
import app.reseam.manager.ui.theme.Opacity
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.theme.extendedColors

@Composable
fun AppHeader(app: AppLook, supporting: String?, modifier: Modifier = Modifier, compact: Boolean = false) {
    if (compact) {
        Row(
            modifier = modifier.fillMaxWidth().padding(start = Layout.margin, end = Layout.margin, top = Space.sm, bottom = Space.xxl),
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app, Sizes.appIconLg, Modifier.sharedIcon(SharedKeys.appIcon(app.packageName), ContainerEnd.Opened))
            ItemText(
                app.name,
                supporting,
                Modifier.weight(1f),
                titleStyle = MaterialTheme.typography.titleLarge,
                titleModifier = Modifier.sharedText(SharedKeys.appName(app.packageName), ContainerEnd.Opened),
                supportModifier = Modifier.sharedText(SharedKeys.appSupport(app.packageName), ContainerEnd.Opened),
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(start = Layout.margin, end = Layout.margin, top = Space.sm, bottom = Space.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            AppIcon(app, Sizes.appIconXl, Modifier.sharedIcon(SharedKeys.appIcon(app.packageName), ContainerEnd.Opened))
            ItemText(
                app.name,
                supporting,
                titleStyle = MaterialTheme.typography.headlineSmall,
                centered = true,
                titleModifier = Modifier.sharedText(SharedKeys.appName(app.packageName), ContainerEnd.Opened),
                supportModifier = Modifier.sharedText(SharedKeys.appSupport(app.packageName), ContainerEnd.Opened),
            )
        }
    }
}

sealed interface RunPhase {
    data class Working(val progress: Float?) : RunPhase
    data object Succeeded : RunPhase
    /** Finished, but without some of the chosen patches. */
    data object Partial : RunPhase
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
            RunPhase.Succeeded -> ResultRing(app, MaterialTheme.colorScheme.primary, MaterialTheme.extendedColors.haloSuccess)
            RunPhase.Partial -> ResultRing(app, MaterialTheme.extendedColors.warning, MaterialTheme.extendedColors.haloWarning)
            RunPhase.Failed -> ResultRing(app, MaterialTheme.colorScheme.error, MaterialTheme.extendedColors.haloFailure)
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
    Box(Modifier.size(Sizes.progressPatching), contentAlignment = Alignment.Center) {
        // Its own layer keeps the per-frame progress animation from redrawing the rest of the screen.
        Canvas(Modifier.fillMaxSize().then(if (progress == null) Modifier.spinning(1400) else Modifier.graphicsLayer())) {
            val stroke = Borders.indicator.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(colors.secondaryContainer, 0f, 360f, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke))
            val sweep = if (progress == null) 90f else 360f * shown
            drawArc(colors.primary, -90f, sweep, useCenter = false, topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        AppIcon(app, Sizes.appIconPatching)
    }
}

@Composable
private fun ResultRing(app: AppLook, ring: Color, halo: Color) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(Sizes.progressResult).drawWithCache {
            val center = size.center
            val radius = size.minDimension * 0.6f
            val glow = Paint().dithered().apply {
                shader = RadialGradientShader(center, radius, listOf(halo.copy(alpha = Opacity.halo), Color.Transparent), listOf(0.7f, 1f))
            }
            val stroke = Borders.ring.toPx()
            onDrawBehind {
                drawIntoCanvas { it.drawCircle(center, radius, glow) }
                drawCircle(colors.surface)
                drawCircle(ring, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(app, Sizes.appIconHero)
    }
}
