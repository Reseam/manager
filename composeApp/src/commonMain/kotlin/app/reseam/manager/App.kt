package app.reseam.manager

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.platform.allGranted
import app.reseam.manager.platform.rememberPermissions
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.nav.AppNavigation
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.theme.ReseamTheme
import kotlinx.coroutines.delay

private val MaxContentWidth = 480.dp

@Composable
fun ReseamApp(graph: AppGraph, versionLabel: String) {
    LaunchedEffect(graph) {
        graph.syncOfficialBundle()
        graph.checkManagerUpdate()
    }
    CompositionLocalProvider(LocalAppGraph provides graph) {
        ReseamTheme {
            val permissions = rememberPermissions()
            // Read once: the gate decides the entry route, it must not re-route mid-session as grants land.
            val startRoute = remember { if (permissions != null && !permissions.allGranted) Route.Permissions else Route.Home }
            Box(
                modifier = Modifier.fillMaxSize().background(ReseamTheme.colors.background),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    NoticeBanner(graph.notices)
                    Box(Modifier.weight(1f)) { AppNavigation(versionLabel, permissions, startRoute) }
                }
            }
        }
    }
}

@Composable
private fun NoticeBanner(notices: Notices) {
    val notice by notices.notice.collectAsStateWithLifecycle()
    val motion = ReseamTheme.motion
    LaunchedEffect(notice) {
        val current = notice ?: return@LaunchedEffect
        delay(8_000)
        notices.dismiss(current)
    }
    AnimatedVisibility(
        visible = notice != null,
        enter = fadeIn(motion.tweenBase()) + slideInVertically(motion.tweenBase()) { -it },
        exit = fadeOut(motion.tweenFast()) + slideOutVertically(motion.tweenFast()) { -it },
    ) {
        val current = notice ?: return@AnimatedVisibility
        Banner(
            message = current.message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            trailing = { IconButton(Icons.Close, "Dismiss", onClick = { notices.dismiss(current) }, size = 28.dp) },
        )
    }
}
