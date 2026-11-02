package app.reseam.manager.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Badge
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.platform.rememberApkPicker
import app.reseam.manager.resources.*
import app.reseam.manager.ui.HumanCheckSheet
import app.reseam.manager.ui.NoPatches
import app.reseam.manager.ui.ShownUpdate
import app.reseam.manager.ui.announcements.AnnouncementCard
import app.reseam.manager.ui.components.BottomBarLayout
import app.reseam.manager.ui.components.BrandTopBar
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.IconButtonStyle
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.SearchBar
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpen: (Route) -> Unit, onSettings: () -> Unit, onUpdates: () -> Unit) {
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
    val sourcesOff by viewModel.sourcesOff.collectAsStateWithLifecycle()
    val appUpdates by viewModel.appUpdates.collectAsStateWithLifecycle()
    val pickFile = rememberApkPicker { viewModel.open(it, onOpen) }
    val selected = (LocalDetailRoute.current as? Route.App)?.packageName
    var checking by remember { mutableStateOf<ShownUpdate?>(null) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).windowInsetsPadding(WindowInsets.statusBars)) {
        BrandTopBar(stringResource(Res.string.app_name)) {
            Box {
                IconButton(Icons.Megaphone, stringResource(Res.string.settings_updates), onUpdates)
                if (announcement != null) Badge(Modifier.align(Alignment.TopEnd).padding(Space.md), containerColor = MaterialTheme.colorScheme.primary)
            }
        }
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
                    sourcesOff && state.patched.isEmpty() -> StateMessage(
                        icon = Icons.Layers,
                        title = stringResource(Res.string.home_sources_off_title),
                        body = null,
                        modifier = Modifier.padding(padding),
                        action = DialogAction(stringResource(Res.string.home_sources_off_action), { onOpen(Route.Sources) }),
                    )
                    state.noPatches && state.patched.isEmpty() -> NoPatches(update != null, syncFailure, viewModel::retrySync, Modifier.padding(padding))
                    state.empty && query.isNotBlank() -> StateMessage(Icons.Search, stringResource(Res.string.home_no_match, query.trim()), null, Modifier.padding(padding))
                    state.empty && state.universal == 0 ->
                        StateMessage(Icons.Layers, stringResource(Res.string.home_empty_title), stringResource(Res.string.home_empty_body), Modifier.padding(padding))
                    else -> AppList(
                        state = state,
                        updates = appUpdates,
                        canShowAll = viewModel.canShowAll,
                        padding = padding,
                        selected = selected,
                        onShowAll = viewModel::showAll,
                        onHideOthers = viewModel::hideOthers,
                        onUpdate = { if (it.humanCheck != null) checking = it else viewModel.updateApp(it) },
                        onUpdateAll = viewModel::updateAllApps,
                    ) { onOpen(Route.App(it)) }
                }
            }
        }
    }
    checking?.let { update ->
        update.humanCheck?.let { url ->
            HumanCheckSheet(
                url,
                onVerified = {
                    checking = null
                    viewModel.updateApp(update)
                },
                onDismiss = { checking = null },
            )
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
fun HomePlaceholder() {
    StateMessage(Icons.Patch, stringResource(Res.string.home_placeholder), null, Modifier.background(MaterialTheme.colorScheme.surfaceContainer))
}
