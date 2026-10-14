package app.reseam.manager.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.platform.rememberApkPicker
import app.reseam.manager.resources.*
import app.reseam.manager.ui.NoPatches
import app.reseam.manager.ui.announcements.AnnouncementCard
import app.reseam.manager.ui.components.AppCard
import app.reseam.manager.ui.components.AppRow
import app.reseam.manager.ui.components.BottomBarLayout
import app.reseam.manager.ui.components.BrandTopBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.FeaturedHeader
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.IconButtonStyle
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.SearchBar
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.appCardShape
import app.reseam.manager.ui.nav.ContainerEnd
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.nav.SharedKeys
import app.reseam.manager.ui.nav.sharedContainer
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.versionLabel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpen: (Route) -> Unit, onSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val picking by viewModel.picking.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsStateWithLifecycle()
    val bundleUpdate by viewModel.bundleUpdate.collectAsStateWithLifecycle()
    val syncFailure by viewModel.syncFailure.collectAsStateWithLifecycle()
    val announcement by viewModel.announcement.collectAsStateWithLifecycle()
    val bundleOffers by viewModel.bundleOffers.collectAsStateWithLifecycle()
    val whatsNew by viewModel.whatsNew.collectAsStateWithLifecycle()
    val pickFile = rememberApkPicker { viewModel.open(it, onOpen) }
    val selected = (LocalDetailRoute.current as? Route.App)?.packageName
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).windowInsetsPadding(WindowInsets.statusBars)) {
        BrandTopBar(stringResource(Res.string.app_name))
        Column(Modifier.padding(horizontal = Layout.marginHome)) {
            val card = Modifier.padding(bottom = Space.sm)
            update?.let { UpdateCard(it, viewModel.updateInstallable, viewModel::installUpdate, viewModel::cancelUpdate, card) }
            bundleUpdate?.let { BundleUpdateCard(it, card) }
            bundleOffers.forEach { offer -> BundleOfferCard(offer, onUpdate = { viewModel.updatePatches(offer) }, onOpen = { onOpen(Route.Releases(offer.bundleId)) }, card) }
            whatsNew.forEach { bundle ->
                bundle.whatsNew?.let { changes ->
                    WhatsNewCard(bundle, changes, onOpen = { onOpen(Route.WhatsNew(bundle.id)) }, onDismiss = { viewModel.dismissWhatsNew(bundle.id) }, card)
                }
            }
            announcement?.let { AnnouncementCard(it, onOpen = { onOpen(Route.Announcement(it.id)) }, onDismiss = { viewModel.dismissAnnouncement(it.id) }, card) }
        }
        BottomBarLayout(
            bar = { HomeBar(query, viewModel::setQuery, onSettings, pickFile, picking) },
            modifier = Modifier.weight(1f),
            margin = Layout.marginHome,
        ) { padding ->
            val pull = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
                state = pull,
                indicator = {
                    val colors = MaterialTheme.colorScheme
                    PullToRefreshDefaults.Indicator(pull, refreshing, Modifier.align(Alignment.TopCenter), containerColor = colors.primaryContainer, color = colors.onPrimaryContainer)
                },
            ) {
                when {
                    !state.loaded -> Loading(Modifier.padding(padding))
                    state.noPatches && state.patched.isEmpty() -> NoPatches(update != null, syncFailure, viewModel::retrySync, Modifier.padding(padding))
                    state.empty && query.isNotBlank() -> StateMessage(Icons.Search, stringResource(Res.string.home_no_match, query.trim()), null, Modifier.padding(padding))
                    state.empty && state.universal == 0 ->
                        StateMessage(Icons.Layers, stringResource(Res.string.home_empty_title), stringResource(Res.string.home_empty_body), Modifier.padding(padding))
                    else -> AppList(state, viewModel.canShowAll, padding, selected, viewModel::showAll, viewModel::hideOthers) { onOpen(Route.App(it)) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.HomeBar(query: String, onQueryChange: (String) -> Unit, onSettings: () -> Unit, onPickFile: () -> Unit, picking: Boolean) {
    var searching by remember { mutableStateOf(false) }
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(!searching) {
            Row {
                IconButton(Icons.Settings, stringResource(Res.string.settings), onSettings, style = IconButtonStyle.Tonal, size = Sizes.buttonLg)
                Spacer(Modifier.width(Space.md))
            }
        }
        SearchBar(query, onQueryChange, stringResource(Res.string.home_search), Modifier.weight(1f), onFocusChange = { searching = it })
        AnimatedVisibility(!searching) {
            Row {
                Spacer(Modifier.width(Space.md))
                IconButton(Icons.FolderOpen, stringResource(Res.string.home_pick_file), onPickFile, style = IconButtonStyle.Tonal, size = Sizes.buttonLg, enabled = !picking)
            }
        }
    }
}

@Composable
private fun AppList(state: HomeState, canShowAll: Boolean, padding: PaddingValues, selected: String?, onShowAll: () -> Unit, onHideOthers: () -> Unit, onOpen: (String) -> Unit) {
    val patchedTitle = stringResource(Res.string.home_patched_title)
    val readyTitle = stringResource(Res.string.home_ready_title)
    val readySupporting = stringResource(Res.string.home_ready_supporting)
    val notInstalledTitle = stringResource(Res.string.home_not_installed_title)
    val notInstalledSupporting = stringResource(Res.string.home_not_installed_supporting)
    val othersTitle = stringResource(Res.string.home_others)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Space.lg, bottom = padding.calculateBottomPadding() + Space.lg),
    ) {
        var first = true
        fun section(key: String, title: String, supporting: String?, apps: List<HomeApp>, action: (@Composable () -> Unit)? = null, entry: @Composable (HomeApp) -> Unit) {
            if (apps.isEmpty()) return
            val top = if (first) 0.dp else Space.xxxl
            first = false
            item(key = key) {
                Row(Modifier.padding(start = Layout.marginHome, end = Layout.marginHome - Space.md, top = top), verticalAlignment = Alignment.CenterVertically) {
                    FeaturedHeader(title, supporting, Modifier.weight(1f))
                    action?.invoke()
                }
            }
            itemsIndexed(apps, key = { _, app -> key + ":" + app.look.packageName }) { index, app ->
                Box(Modifier.padding(top = if (index == 0) Space.sm else Space.md)) { entry(app) }
            }
        }
        section("patched", patchedTitle, null, state.patched) { HomeCard(it, selected, onOpen) }
        section("ready", readyTitle, readySupporting, state.ready) { HomeCard(it, selected, onOpen) }
        section("notInstalled", notInstalledTitle, notInstalledSupporting, state.notInstalled) { HomeRow(it, selected, onOpen) }
        val others = state.others
        if (others != null) {
            section("others", othersTitle, null, others, action = { Button(stringResource(Res.string.home_hide_others), onHideOthers, style = ButtonStyle.Text) }) { HomeRow(it, selected, onOpen) }
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

@Composable
fun HomePlaceholder() {
    StateMessage(Icons.Patch, stringResource(Res.string.home_placeholder), null, Modifier.background(MaterialTheme.colorScheme.surfaceContainer))
}
