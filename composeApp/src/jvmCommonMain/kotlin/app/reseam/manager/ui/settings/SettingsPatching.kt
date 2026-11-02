package app.reseam.manager.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalUriHandler
import app.reseam.manager.data.Settings
import app.reseam.manager.platform.Permission
import app.reseam.manager.platform.Permissions
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.ExternalMark
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.nav.Route
import org.jetbrains.compose.resources.stringResource

// Opens Play Protect in the Play Store; its switches sit behind the gear, which nothing links to.
private const val PlayProtectUrl = "https://play.google.com/protect/home"

/** Patch sources, and how patches and patched apps update. */
fun LazyListScope.patchingSection(
    viewModel: SettingsViewModel,
    settings: Settings,
    permissions: Permissions?,
    asksBeforeUpdates: Boolean,
    sourcesOpen: Boolean,
    onOpen: (Route) -> Unit,
    onAutoUpdateApps: (Boolean) -> Unit,
) {
    pageSection(Res.string.settings_patching)
    itemGroup {
        item { shape ->
            GroupedItem(
                title = stringResource(Res.string.settings_sources),
                supporting = stringResource(Res.string.settings_sources_supporting),
                shape = shape,
                icon = Icons.Layers,
                selected = sourcesOpen,
                onClick = { onOpen(Route.Sources) },
                trailing = { Chevron() },
            )
        }
        item { shape ->
            val supporting = if (settings.autoUpdateBundles) Res.string.settings_update_supporting else Res.string.settings_update_supporting_off
            SwitchItem(stringResource(Res.string.settings_update), settings.autoUpdateBundles, viewModel::setAutoUpdateBundles, shape, supporting = stringResource(supporting), icon = Icons.Refresh)
        }
        if (viewModel.canAutoUpdateApps) {
            item { shape -> AutoUpdateAppsItem(settings.autoUpdateApps, permissions, onAutoUpdateApps, shape) }
            if (settings.autoUpdateApps && asksBeforeUpdates) {
                item { shape ->
                    val uriHandler = LocalUriHandler.current
                    GroupedItem(
                        stringResource(Res.string.settings_play_protect),
                        shape,
                        supporting = stringResource(Res.string.settings_play_protect_supporting),
                        icon = Icons.ShieldCheck,
                        onClick = { uriHandler.openUri(PlayProtectUrl) },
                        trailing = { ExternalMark() },
                    )
                }
            }
        }
        item { shape ->
            SwitchItem(stringResource(Res.string.settings_other_versions), settings.allowIncompatiblePatches, viewModel::setAllowIncompatiblePatches, shape, supporting = stringResource(Res.string.settings_other_versions_supporting), icon = Icons.Warning)
        }
    }
}

/** Shown as auto-update turns on while Play Protect would ask before each install. */
@Composable
fun PlayProtectDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Dialog(
        headline = stringResource(Res.string.settings_play_protect),
        body = stringResource(Res.string.play_protect_dialog_body),
        icon = Icons.ShieldCheck,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(Res.string.play_protect_keep), onDismiss),
            DialogAction(stringResource(Res.string.play_protect_open), {
                uriHandler.openUri(PlayProtectUrl)
                onDismiss()
            }),
        ),
    )
}

/** Apps that change in the background have to be able to say so: the switch turns on once notifications are allowed. */
@Composable
private fun AutoUpdateAppsItem(enabled: Boolean, permissions: Permissions?, onChange: (Boolean) -> Unit, shape: Shape) {
    val notifying = permissions == null || Permission.Notifications in permissions.granted
    var onWhenAllowed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(notifying, onWhenAllowed) {
        if (onWhenAllowed && notifying) {
            onWhenAllowed = false
            onChange(true)
        }
    }
    val on = enabled && notifying
    SwitchItem(
        stringResource(Res.string.settings_update_apps),
        on,
        { turnOn ->
            if (turnOn && !notifying) {
                onWhenAllowed = true
                permissions?.request(Permission.Notifications)
            } else {
                onChange(turnOn)
            }
        },
        shape,
        supporting = stringResource(if (on) Res.string.settings_update_apps_supporting else Res.string.settings_update_apps_supporting_off),
        icon = Icons.Patch,
    )
}
