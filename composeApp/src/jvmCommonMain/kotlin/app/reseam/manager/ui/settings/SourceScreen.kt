package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.resources.*
import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.ConfirmDialog
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.fingerprint
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SourceScreen(viewModel: SourceViewModel, onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val bundle by viewModel.bundle.collectAsStateWithLifecycle()
    val patches by viewModel.patches.collectAsStateWithLifecycle()
    val autoUpdates by viewModel.autoUpdates.collectAsStateWithLifecycle()
    val open = LocalDetailRoute.current
    var removing by rememberSaveable { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val current = bundle ?: return

    SettingsPage(current.name, onBack) {
        itemGroup {
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.source_version),
                    shape = shape,
                    supporting = current.version ?: stringResource(Res.string.source_version_none),
                    icon = Icons.Package,
                    selected = open is Route.Releases,
                    onClick = { onOpen(Route.Releases(current.id)) }.takeIf { current.followsUpdates },
                    trailing = if (current.followsUpdates) { { Chevron() } } else null,
                )
            }
            item { shape ->
                val link = current.origin.takeIf { it.startsWith("https://") || it.startsWith("http://") }
                GroupedItem(
                    title = stringResource(Res.string.source_from),
                    shape = shape,
                    supporting = current.origin,
                    icon = Icons.Source,
                    onClick = link?.let { { uriHandler.openUri(it) } },
                    trailing = link?.let { { Chevron() } },
                )
            }
            item { shape ->
                if (current.official) {
                    GroupedItem(stringResource(Res.string.source_official), shape, supporting = stringResource(Res.string.source_official_supporting), icon = Icons.ShieldCheck)
                } else {
                    GroupedItem(stringResource(Res.string.source_signer), shape, supporting = current.id.fingerprint(), icon = Icons.ShieldCheck)
                }
            }
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.source_updates),
                    supporting = stringResource(
                        when {
                            !current.followsUpdates -> Res.string.source_updates_off
                            autoUpdates -> Res.string.source_updates_on
                            else -> Res.string.source_updates_ask
                        },
                    ),
                    shape = shape,
                    icon = Icons.Refresh,
                )
            }
            patches?.let { list ->
                val apps = list.flatMap { it.declared }.map { it.`package` }.distinct().size
                item { shape ->
                    val count = pluralStringResource(Res.plurals.source_apps_count, apps, apps)
                    GroupedItem(
                        title = stringResource(Res.string.source_apps),
                        supporting = if (list.any { it.universal }) stringResource(Res.string.source_apps_and_any, count) else count,
                        shape = shape,
                        icon = Icons.Phone,
                    )
                }
            }
        }
        if (!current.official) {
            item {
                Button(
                    label = stringResource(Res.string.source_remove),
                    onClick = { removing = true },
                    modifier = Modifier.fillMaxWidth().padding(top = Space.md),
                    style = ButtonStyle.Outlined,
                    icon = Icons.Trash,
                    destructive = true,
                )
            }
        }
        pageSection(Res.string.app_patches)
        val list = patches
        if (list == null) {
            item { Loading() }
        } else {
            itemGroup {
                list.forEach { patch ->
                    item(key = patch.spec.id) { shape ->
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

    if (removing) {
        ConfirmDialog(
            headline = stringResource(Res.string.source_remove_title, current.name),
            body = stringResource(Res.string.source_remove_body),
            icon = Icons.Trash,
            confirm = stringResource(Res.string.remove),
            onConfirm = { viewModel.remove(onBack) },
            onDismiss = { removing = false },
        )
    }
}
