package app.reseam.manager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.reseam.manager.AppGraph
import app.reseam.manager.Notices
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.IconButtonStyle
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.nav.AppNavigation
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Motion
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.ReseamTheme
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val NoticeMillis = 6_000L

/** The activity outlives folds and rotations, so every return to the foreground counts as a launch. */
@Composable
fun ReseamApp(graph: AppGraph, dark: Boolean = isSystemInDarkTheme()) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(graph, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { graph.announcements.refresh() }
            if (graph.managerUpdates.check() == null) graph.bundleSyncer.sync()
        }
    }
    ReseamTheme(dark) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            AppNavigation(graph)
            NoticeHost(graph.notices, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun NoticeHost(notices: Notices, modifier: Modifier) {
    val posted by notices.posted.collectAsStateWithLifecycle()
    LaunchedEffect(posted) {
        val current = posted ?: return@LaunchedEffect
        delay(NoticeMillis)
        notices.dismiss(current)
    }
    AnimatedVisibility(
        visible = posted != null,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(start = Layout.margin, end = Layout.margin, bottom = Sizes.searchBar + Space.xxxl),
        enter = fadeIn(tween(Motion.short4)) + slideInVertically(tween(Motion.medium2, easing = Motion.emphasizedDecelerate)) { it / 2 },
        exit = fadeOut(tween(Motion.short4)) + slideOutVertically(tween(Motion.short4, easing = Motion.emphasizedAccelerate)) { it / 2 },
    ) {
        val current = posted ?: return@AnimatedVisibility
        Surface(Modifier.widthIn(max = 560.dp), shape = RoundedCornerShape(Radius.xs), color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
            Row(Modifier.padding(start = Space.lg, end = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(current.notice.text(), Modifier.weight(1f, fill = false).padding(vertical = Space.md + Space.xxs), style = MaterialTheme.typography.bodyMedium)
                IconButton(Icons.Close, stringResource(Res.string.dismiss), { notices.dismiss(current) }, style = IconButtonStyle.Inverse)
            }
        }
    }
}
