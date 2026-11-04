package app.reseam.manager.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import app.reseam.manager.resources.*
import app.reseam.manager.ui.ShownUpdate
import app.reseam.manager.ui.components.AppCard
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.components.AppRow
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.FeaturedHeader
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.appCardShape
import app.reseam.manager.ui.nav.ContainerEnd
import app.reseam.manager.ui.nav.SharedKeys
import app.reseam.manager.ui.nav.sharedContainer
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.versionLabel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Home's apps in sections: updates, patched apps, apps ready to patch, ones not installed, and every other app on request. */
@Composable
fun AppList(
    state: HomeState,
    updates: List<ShownUpdate>,
    canShowAll: Boolean,
    padding: PaddingValues,
    selected: String?,
    onShowAll: () -> Unit,
    onHideOthers: () -> Unit,
    onUpdate: (ShownUpdate) -> Unit,
    onUpdateAll: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val updatesTitle = stringResource(Res.string.home_updates_title)
    val patchedTitle = stringResource(Res.string.home_patched_title)
    val readyTitle = stringResource(Res.string.home_ready_title)
    val readySupporting = stringResource(Res.string.home_ready_supporting)
    val notInstalledTitle = stringResource(Res.string.home_not_installed_title)
    val notInstalledSupporting = stringResource(Res.string.home_not_installed_supporting)
    val othersTitle = stringResource(Res.string.home_others)
    val list = rememberLazyListState()
    // Updates arrive after the list first draws and go above it; a list at the top stays there so they show.
    if (list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0) list.requestScrollToItem(0)
    LazyColumn(
        state = list,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Space.lg, bottom = padding.calculateBottomPadding() + Space.lg),
    ) {
        var first = true
        fun <T> section(
            key: String,
            title: String,
            supporting: String?,
            apps: List<T>,
            look: (T) -> AppLook,
            action: (@Composable () -> Unit)? = null,
            entry: @Composable (T) -> Unit,
        ) {
            if (apps.isEmpty()) return
            val top = if (first) 0.dp else Space.xxxl
            first = false
            item(key = key) {
                Row(Modifier.padding(start = Layout.marginHome, end = Layout.marginHome - Space.md, top = top), verticalAlignment = Alignment.CenterVertically) {
                    FeaturedHeader(title, supporting, Modifier.weight(1f))
                    action?.invoke()
                }
            }
            itemsIndexed(apps, key = { _, app -> key + ":" + look(app).packageName }) { index, app ->
                Box(Modifier.padding(top = if (index == 0) Space.sm else Space.md)) { entry(app) }
            }
        }
        val updateAll = updates.count { it.joinsUpdateAll } > 1
        section("updates", updatesTitle, null, updates, { it.look }, action = { if (updateAll) Button(stringResource(Res.string.home_update_all), onUpdateAll, style = ButtonStyle.Text) }) { update ->
            val packageName = update.look.packageName
            AppUpdateCard(
                update = update,
                onUpdate = { onUpdate(update) },
                onOpen = { onOpen(packageName) },
                modifier = Modifier.padding(horizontal = Layout.marginHome).sharedContainer(SharedKeys.app(packageName), appCardShape(packageName == selected), ContainerEnd.Opener),
                selected = packageName == selected,
            )
        }
        val updating = updates.map { it.look.packageName }.toSet()
        section("patched", patchedTitle, null, state.patched.filter { it.look.packageName !in updating }, { it.look }) { HomeCard(it, selected, onOpen) }
        section("ready", readyTitle, readySupporting, state.ready, { it.look }) { HomeCard(it, selected, onOpen) }
        section("notInstalled", notInstalledTitle, notInstalledSupporting, state.notInstalled, { it.look }) { HomeRow(it, selected, onOpen) }
        val others = state.others
        if (others != null) {
            section("others", othersTitle, null, others, { it.look }, action = { Button(stringResource(Res.string.home_hide_others), onHideOthers, style = ButtonStyle.Text) }) { HomeRow(it, selected, onOpen) }
        } else if (canShowAll && state.universal > 0) {
            item(key = "showAll") {
                Box(Modifier.padding(start = Layout.marginHome, top = Space.xxxl)) {
                    Button(stringResource(Res.string.home_show_all), onShowAll, style = ButtonStyle.Tonal, size = ButtonSize.Medium, icon = Icons.ChevronDown)
                }
            }
        }
    }
}

@Composable
private fun HomeCard(app: HomeApp, selected: String?, onOpen: (String) -> Unit) {
    val packageName = app.look.packageName
    AppCard(
        app = app.look,
        supporting = when {
            app.saved -> stringResource(Res.string.home_saved, versionLabel(app.versionName))
            app.applied == null -> app.versionName?.let { versionLabel(it) } ?: stringResource(Res.string.source_newest)
            app.mounted -> pluralStringResource(Res.plurals.home_mounted, app.applied, app.applied)
            else -> pluralStringResource(Res.plurals.home_applied, app.applied, app.applied)
        },
        onClick = { onOpen(packageName) },
        modifier = Modifier.padding(horizontal = Layout.marginHome).sharedContainer(SharedKeys.app(packageName), appCardShape(packageName == selected), ContainerEnd.Opener),
        selected = packageName == selected,
    )
}

@Composable
private fun HomeRow(app: HomeApp, selected: String?, onOpen: (String) -> Unit) {
    val count = app.patchCount
    val supporting = when {
        count == null -> stringResource(Res.string.home_universal)
        app.versionName != null -> pluralStringResource(Res.plurals.home_version_patches, count, count, app.versionName)
        else -> pluralStringResource(Res.plurals.home_patches, count, count)
    }
    AppRow(
        app = app.look,
        supporting = supporting,
        onClick = { onOpen(app.look.packageName) },
        modifier = Modifier.sharedContainer(SharedKeys.app(app.look.packageName), RectangleShape, ContainerEnd.Opener),
        margin = Layout.marginHome,
        selected = app.look.packageName == selected,
    )
}
