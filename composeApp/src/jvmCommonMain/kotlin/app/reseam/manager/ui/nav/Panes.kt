package app.reseam.manager.ui.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import app.reseam.manager.ui.theme.Motion

val TwoPaneWidth = 720.dp

private val MaxListWidth = 440.dp

enum class Pane { Single, List, Detail }

val LocalPane = staticCompositionLocalOf { Pane.Single }

val LocalDetailRoute = staticCompositionLocalOf<NavKey?> { null }

private data object PaneRolesKey : NavMetadataKey<PaneRoles>

private data class PaneRoles(val route: NavKey, val list: Boolean, val detail: Boolean)

fun paneRoles(route: NavKey, list: Boolean = false, detail: Boolean = false) = metadata { put(PaneRolesKey, PaneRoles(route, list, detail)) }

private val NavEntry<*>.roles: PaneRoles? get() = metadata[PaneRolesKey]

private val NavEntry<*>.route: NavKey get() = checkNotNull(roles).route

private class TwoPaneScene(
    private val list: NavEntry<NavKey>,
    private val detail: NavEntry<NavKey>?,
    override val previousEntries: List<NavEntry<NavKey>>,
    private val placeholder: @Composable (NavKey) -> Unit,
) : Scene<NavKey> {
    override val key: Any get() = list.contentKey
    override val entries: List<NavEntry<NavKey>> get() = listOfNotNull(list, detail)
    override val content: @Composable () -> Unit = {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val listWidth = minOf(maxWidth / 2, MaxListWidth)
            Row(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalPane provides Pane.List, LocalDetailRoute provides detail?.route) {
                    Box(Modifier.width(listWidth).fillMaxHeight()) { list.Content() }
                }
                AnimatedContent(
                    targetState = detail,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentKey = { it?.contentKey },
                    transitionSpec = { fadeIn(tween(Motion.medium2)) togetherWith fadeOut(tween(Motion.short4)) },
                ) { shown ->
                    CompositionLocalProvider(LocalPane provides Pane.Detail) {
                        if (shown != null) shown.Content() else placeholder(list.route)
                    }
                }
            }
        }
    }
}

class TwoPaneStrategy(private val placeholder: @Composable (list: NavKey) -> Unit) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val last = entries.last()
        val under = entries.getOrNull(entries.lastIndex - 1)
        return when {
            last.roles?.detail == true && under?.roles?.list == true -> TwoPaneScene(under, last, entries.dropLast(1), placeholder)
            last.roles?.list == true -> TwoPaneScene(last, null, entries.dropLast(1), placeholder)
            else -> null
        }
    }
}
