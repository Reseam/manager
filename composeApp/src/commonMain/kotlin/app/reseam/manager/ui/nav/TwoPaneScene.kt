package app.reseam.manager.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import app.reseam.manager.ui.components.LocalPaneRole
import app.reseam.manager.ui.components.PaneRole
import app.reseam.manager.ui.components.TwoPane

data object PaneRoleKey : NavMetadataKey<PaneRole>

fun paneRole(role: PaneRole) = metadata { put(PaneRoleKey, role) }

/** A list entry beside the detail it opened, or beside [placeholder] until one is chosen. */
class TwoPaneScene<T : Any>(
    private val list: NavEntry<T>,
    private val detail: NavEntry<T>?,
    override val previousEntries: List<NavEntry<T>>,
    private val placeholder: @Composable () -> Unit,
) : Scene<T> {
    override val key: Any get() = list.contentKey
    override val entries: List<NavEntry<T>> get() = listOfNotNull(list, detail)
    override val content: @Composable () -> Unit = {
        TwoPane(
            list = { CompositionLocalProvider(LocalPaneRole provides PaneRole.List) { list.Content() } },
            detail = { CompositionLocalProvider(LocalPaneRole provides PaneRole.Detail) { if (detail != null) detail.Content() else placeholder() } },
        )
    }
}

/** Pairs an entry marked [PaneRole.Detail] with the [PaneRole.List] entry under it; a list on top gets the placeholder. */
class TwoPaneSceneStrategy<T : Any>(private val placeholder: @Composable (listKey: Any) -> Unit) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.last()
        return when (last.metadata[PaneRoleKey]) {
            PaneRole.List -> TwoPaneScene(last, null, entries.dropLast(1)) { placeholder(last.contentKey) }
            PaneRole.Detail -> {
                val list = entries.getOrNull(entries.lastIndex - 1)?.takeIf { it.metadata[PaneRoleKey] == PaneRole.List } ?: return null
                TwoPaneScene(list, last, entries.dropLast(1)) { placeholder(list.contentKey) }
            }
            else -> null
        }
    }
}
