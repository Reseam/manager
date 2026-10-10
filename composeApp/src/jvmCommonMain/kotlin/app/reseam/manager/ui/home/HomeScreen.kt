package app.reseam.manager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.FeaturedHeader
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.IconButtonStyle
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.SearchBar
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.nav.SharedKeys
import app.reseam.manager.ui.nav.shared
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.versionLabel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpen: (Route) -> Unit, onSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val picking by viewModel.picking.collectAsStateWithLifecycle()
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
        Column(Modifier.padding(horizontal = Layout.marginHome), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            update?.let { UpdateCard(it, viewModel.updateInstallable, viewModel::installUpdate, viewModel::cancelUpdate) }
            bundleUpdate?.let { BundleUpdateCard(it) }
            bundleOffers.forEach { offer -> BundleOfferCard(offer, onUpdate = { viewModel.updatePatches(offer) }, onOpen = { onOpen(Route.Releases(offer.bundleId)) }) }
            whatsNew.forEach { bundle ->
                bundle.whatsNew?.let { changes ->
                    WhatsNewCard(bundle, changes, onOpen = { onOpen(Route.WhatsNew(bundle.id)) }, onDismiss = { viewModel.dismissWhatsNew(bundle.id) })
                }
            }
            announcement?.let { AnnouncementCard(it, onOpen = { onOpen(Route.Announcement(it.id)) }, onDismiss = { viewModel.dismissAnnouncement(it.id) }) }
        }
        BottomBarLayout(
            bar = {
                IconButton(Icons.Settings, stringResource(Res.string.settings), onSettings, style = IconButtonStyle.Tonal, size = Sizes.buttonLg)
                SearchBar(query, viewModel::setQuery, stringResource(Res.string.home_search), Modifier.weight(1f))
                IconButton(Icons.FolderOpen, stringResource(Res.string.home_pick_file), pickFile, style = IconButtonStyle.Tonal, size = Sizes.buttonLg, enabled = !picking)
            },
            modifier = Modifier.weight(1f),
            margin = Layout.marginHome,
        ) { padding ->
            when {
                !state.loaded -> Loading(Modifier.padding(padding))
                state.noPatches && state.patched.isEmpty() -> NoPatches(update != null, syncFailure, viewModel::retrySync, Modifier.padding(padding))
                state.empty && query.isNotBlank() -> StateMessage(Icons.Search, stringResource(Res.string.home_no_match, query.trim()), null, Modifier.padding(padding))
                state.empty && state.universal == 0 ->
                    StateMessage(Icons.Layers, stringResource(Res.string.home_empty_title), stringResource(Res.string.home_empty_body), Modifier.padding(padding))
                else -> AppList(state, viewModel.canShowAll, padding, selected, viewModel::showAll) { onOpen(Route.App(it)) }
            }
        }
    }
}

@Composable
private fun AppList(state: HomeState, canShowAll: Boolean, padding: PaddingValues, selected: String?, onShowAll: () -> Unit, onOpen: (String) -> Unit) {
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
        fun section(key: String, title: String, supporting: String?, apps: List<HomeApp>, entry: @Composable (HomeApp) -> Unit) {
            if (apps.isEmpty()) return
            val top = if (first) 0.dp else Space.xxxl
            first = false
            item(key = key) { FeaturedHeader(title, supporting, Modifier.padding(horizontal = Layout.marginHome).padding(top = top)) }
            itemsIndexed(apps, key = { _, app -> key + ":" + app.look.packageName }) { index, app ->
                Box(Modifier.padding(top = if (index == 0) Space.sm else Space.md)) { entry(app) }
            }
        }
        section("patched", patchedTitle, null, state.patched) { HomeCard(it, selected, onOpen) }
        section("ready", readyTitle, readySupporting, state.ready) { HomeCard(it, selected, onOpen) }
        section("notInstalled", notInstalledTitle, notInstalledSupporting, state.notInstalled) { HomeRow(it, selected, onOpen) }
        val others = state.others
        if (others != null) {
            section("others", othersTitle, null, others) { HomeRow(it, selected, onOpen) }
        } else if (canShowAll && state.universal > 0) {
            item(key = "showAll") {
                Box(Modifier.padding(start = Layout.marginHome - Space.md, top = Space.xxxl)) {
                    Button(stringResource(Res.string.home_show_all), onShowAll, style = ButtonStyle.Text)
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
            app.applied == null -> app.versionName?.let { versionLabel(it) } ?: stringResource(Res.string.source_newest)
            app.mounted -> pluralStringResource(Res.plurals.home_mounted, app.applied, app.applied)
            else -> pluralStringResource(Res.plurals.home_applied, app.applied, app.applied)
        },
        onClick = { onOpen(packageName) },
        modifier = Modifier.padding(horizontal = Layout.marginHome).shared(SharedKeys.app(packageName)),
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
    AppRow(app.look, supporting, onClick = { onOpen(app.look.packageName) }, margin = Layout.marginHome, selected = app.look.packageName == selected)
}

@Composable
fun HomePlaceholder() {
    StateMessage(Icons.Patch, stringResource(Res.string.home_placeholder), null, Modifier.background(MaterialTheme.colorScheme.surfaceContainer))
}
