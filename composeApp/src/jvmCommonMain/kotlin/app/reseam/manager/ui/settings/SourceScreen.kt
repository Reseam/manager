package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.Bundle
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.ConfirmDialog
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.ExternalMark
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.SearchBar
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.fingerprint
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
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
    val delisted by viewModel.delisted.collectAsStateWithLifecycle()
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
                delisted = delisted,
                releasesOpen = open is Route.Releases,
                onReleases = { onOpen(Route.Releases(current.id)) },
                onSigner = { explainingSigner = true },
                onApps = { listingApps = true },
                onEnabled = viewModel::setEnabled,
                onPrereleases = viewModel::setPrereleases,
                onRemove = { removing = true },
            )
        }
        patchSection(matching, filter, viewModel::setFilter, query, (open as? Route.Patch)?.patchId) { patch ->
            onOpen(Route.Patch(current.id, patch.meta.spec.id))
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
private fun Details(
    bundle: Bundle,
    groups: List<PatchGroup>?,
    delisted: Boolean,
    releasesOpen: Boolean,
    onReleases: () -> Unit,
    onSigner: () -> Unit,
    onApps: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onPrereleases: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val link = bundle.origin.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        if (delisted) {
            ItemGroup(Modifier.padding(bottom = Space.md)) {
                item { shape ->
                    GroupedItem(
                        title = stringResource(Res.string.source_delisted_title),
                        shape = shape,
                        supporting = stringResource(Res.string.source_delisted_body),
                        icon = Icons.Warning,
                        iconTint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        ItemGroup {
            item { shape ->
                SwitchItem(
                    title = stringResource(Res.string.source_enabled),
                    checked = bundle.enabled,
                    onCheckedChange = onEnabled,
                    shape = shape,
                    supporting = if (bundle.enabled) null else stringResource(Res.string.source_enabled_off),
                    icon = Icons.Patch,
                )
            }
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
            if (bundle.followsUpdates) {
                item { shape ->
                    SwitchItem(
                        title = stringResource(Res.string.source_prereleases),
                        checked = bundle.prereleases,
                        onCheckedChange = onPrereleases,
                        shape = shape,
                        supporting = stringResource(Res.string.source_prereleases_supporting),
                        icon = Icons.Download,
                    )
                }
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
