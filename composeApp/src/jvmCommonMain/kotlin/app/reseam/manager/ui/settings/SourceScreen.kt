package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.Bundle
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ConfirmDialog
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.ExternalMark
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.IconBadge
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.SearchBar
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.fingerprint
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.theme.extendedTypography
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val PatchesIndex = 1

@Composable
fun SourceScreen(viewModel: SourceViewModel, onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val bundle by viewModel.bundle.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val matching by viewModel.matching.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val open = LocalDetailRoute.current
    var removing by rememberSaveable { mutableStateOf(false) }
    var explainingSigner by rememberSaveable { mutableStateOf(false) }
    var listingApps by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val current = bundle
    if (current == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    SettingsPage(
        title = current.name,
        onBack = onBack,
        state = listState,
        footer = { SearchBar(query, viewModel::setQuery, stringResource(Res.string.patches_search), Modifier.weight(1f)) },
    ) {
        item {
            Details(
                bundle = current,
                groups = groups,
                releasesOpen = open is Route.Releases,
                onReleases = { onOpen(Route.Releases(current.id)) },
                onSigner = { explainingSigner = true },
                onApps = { listingApps = true },
                onRemove = { removing = true },
            )
        }
        pageSection(Res.string.app_patches)
        val shown = matching
        if (shown == null) {
            item { Loading() }
        } else {
            item { AppFilters(shown, filter, viewModel::setFilter) }
            val visible = shown.filter { filter.shows(it) && it.patches.isNotEmpty() }
            if (visible.isEmpty()) {
                item { StateMessage(Icons.Search, stringResource(Res.string.patches_no_match, query.trim()), null) }
            }
            visible.forEach { group ->
                val groupKey = group.app?.packageName ?: ""
                item(key = "group:$groupKey") { GroupHeader(group) }
                itemGroup {
                    group.patches.forEach { patch ->
                        item(key = "$groupKey/${patch.spec.id}") { shape ->
                            GroupedItem(
                                title = patch.spec.name,
                                supporting = patch.spec.description.ifEmpty { null },
                                shape = shape,
                                icon = Icons.Patch,
                                selected = (open as? Route.Patch)?.patchId == patch.spec.id,
                                onClick = { onOpen(Route.Patch(current.id, patch.spec.id)) },
                                trailing = { Chevron() },
                            )
                        }
                    }
                }
            }
        }
    }

    if (explainingSigner) SignerDialog(current, onDismiss = { explainingSigner = false })
    if (listingApps) {
        AppsSheet(groups.orEmpty(), onDismiss = { listingApps = false }) { chosen ->
            listingApps = false
            viewModel.setFilter(chosen)
            scope.launch { listState.animateScrollToItem(PatchesIndex) }
        }
    }
    if (removing) {
        ConfirmDialog(
            headline = stringResource(Res.string.source_remove_title, current.name),
            body = stringResource(Res.string.source_remove_body),
            icon = Icons.Trash,
            confirm = stringResource(Res.string.remove),
            onConfirm = viewModel::remove,
            onDismiss = { removing = false },
        )
    }
}

@Composable
private fun PatchGroup.name(): String = app?.name ?: stringResource(Res.string.patches_for_any)

@Composable
private fun AppFilters(groups: List<PatchGroup>, filter: AppFilter, onFilter: (AppFilter) -> Unit) {
    val options = listOf(AppFilter.All to stringResource(Res.string.source_filter, stringResource(Res.string.source_filter_all), groups.sumOf { it.patches.size })) +
        groups.map { group -> AppFilter.Only(group.app?.packageName) to stringResource(Res.string.source_filter, group.name(), group.patches.size) }
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        items(options, key = { (option) -> option.toString() }) { (option, label) ->
            val selected = option == filter
            Chip(label, Modifier.semantics { this.selected = selected }, outlined = !selected, onClick = { onFilter(option) })
        }
    }
}

@Composable
private fun GroupHeader(group: PatchGroup) {
    Row(Modifier.padding(top = Space.lg, bottom = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
        GroupIcon(group, Sizes.iconMd)
        Text(group.name(), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(group.patches.size.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GroupIcon(group: PatchGroup, size: Dp) {
    val app = group.app
    if (app != null) AppIcon(app, size) else IconBadge(Icons.Layers, size)
}

@Composable
private fun AppsSheet(groups: List<PatchGroup>, onDismiss: () -> Unit, onChoose: (AppFilter) -> Unit) {
    Sheet(stringResource(Res.string.source_apps), onDismiss, supporting = stringResource(Res.string.source_apps_body)) {
        ItemGroup {
            groups.forEach { group ->
                item { shape ->
                    GroupedItem(
                        title = group.name(),
                        supporting = pluralStringResource(Res.plurals.home_patches, group.patches.size, group.patches.size),
                        shape = shape,
                        leading = { GroupIcon(group, Sizes.appIconSm) },
                        onClick = { onChoose(AppFilter.Only(group.app?.packageName)) },
                        trailing = { Chevron() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SignerDialog(bundle: Bundle, onDismiss: () -> Unit) {
    Dialog(
        headline = stringResource(if (bundle.official) Res.string.source_official_title else Res.string.source_signer_title),
        body = if (bundle.official) stringResource(Res.string.source_official_body) else stringResource(Res.string.source_signer_body, bundle.name),
        icon = Icons.ShieldCheck,
        onDismiss = onDismiss,
        actions = listOf(DialogAction(stringResource(Res.string.close), onDismiss)),
    ) {
        Text(bundle.id.fingerprint(bundle.id.length), Modifier.fillMaxWidth(), style = MaterialTheme.extendedTypography.monoSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Details(bundle: Bundle, groups: List<PatchGroup>?, releasesOpen: Boolean, onReleases: () -> Unit, onSigner: () -> Unit, onApps: () -> Unit, onRemove: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val link = bundle.origin.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        ItemGroup {
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.source_version),
                    shape = shape,
                    supporting = if (bundle.followsUpdates) bundle.version ?: stringResource(Res.string.source_version_none) else stringResource(Res.string.source_updates_off),
                    icon = Icons.Package,
                    selected = releasesOpen,
                    onClick = onReleases.takeIf { bundle.followsUpdates },
                    trailing = if (bundle.followsUpdates) { { Chevron() } } else null,
                )
            }
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.source_from),
                    shape = shape,
                    supporting = bundle.origin,
                    icon = Icons.Source,
                    onClick = link?.let { { uriHandler.openUri(it) } },
                    trailing = link?.let { { ExternalMark() } },
                )
            }
            item { shape ->
                GroupedItem(
                    title = stringResource(if (bundle.official) Res.string.source_official else Res.string.source_signer),
                    shape = shape,
                    supporting = if (bundle.official) stringResource(Res.string.source_official_supporting) else bundle.id.fingerprint(),
                    supportingStyle = if (bundle.official) MaterialTheme.typography.bodyMedium else MaterialTheme.extendedTypography.monoSmall,
                    icon = Icons.ShieldCheck,
                    onClick = onSigner,
                    trailing = { Chevron() },
                )
            }
            if (groups != null) {
                item { shape ->
                    val apps = groups.count { it.app != null }
                    val count = pluralStringResource(Res.plurals.source_apps_count, apps, apps)
                    GroupedItem(
                        title = stringResource(Res.string.source_apps),
                        supporting = if (groups.any { it.app == null }) stringResource(Res.string.source_apps_and_any, count) else count,
                        shape = shape,
                        icon = Icons.Phone,
                        onClick = onApps,
                        trailing = { Chevron() },
                    )
                }
            }
        }
        if (!bundle.official) {
            Button(
                label = stringResource(Res.string.source_remove),
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth().padding(top = Space.md),
                style = ButtonStyle.Outlined,
                icon = Icons.Trash,
                destructive = true,
            )
        }
    }
}
