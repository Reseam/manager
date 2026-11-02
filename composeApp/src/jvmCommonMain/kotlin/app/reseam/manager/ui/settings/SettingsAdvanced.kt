package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import app.reseam.manager.data.DefaultServerUrl
import app.reseam.manager.data.Settings
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.components.itemGroup
import org.jetbrains.compose.resources.stringResource

/** The server Manager talks to, prereleases of Manager, and the system installer. */
fun LazyListScope.advancedSection(viewModel: SettingsViewModel, settings: Settings, onEditServer: () -> Unit) {
    pageSection(Res.string.settings_advanced)
    itemGroup {
        item { shape ->
            GroupedItem(stringResource(Res.string.settings_server), shape, supporting = settings.serverUrl, icon = Icons.Globe, onClick = onEditServer, trailing = { Chevron() })
        }
        item { shape ->
            SwitchItem(stringResource(Res.string.settings_manager_prereleases), settings.managerPrereleases, viewModel::setManagerPrereleases, shape, supporting = stringResource(Res.string.settings_manager_prereleases_supporting), icon = Icons.Download)
        }
        if (viewModel.installs) {
            item { shape ->
                SwitchItem(stringResource(Res.string.settings_system_installer), settings.useSystemInstaller, viewModel::setUseSystemInstaller, shape, supporting = stringResource(Res.string.settings_system_installer_supporting), icon = Icons.Package)
            }
        }
    }
}

@Composable
fun ServerDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var url by rememberSaveable { mutableStateOf(current) }
    Dialog(
        headline = stringResource(Res.string.settings_server),
        body = stringResource(Res.string.settings_server_body),
        icon = Icons.Globe,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(Res.string.cancel), onDismiss),
            DialogAction(stringResource(Res.string.save), { onSave(url) }, enabled = url.isNotBlank()),
        ),
    ) {
        TextField(
            value = url,
            onValueChange = { url = it },
            label = stringResource(Res.string.settings_server_label),
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Uri,
            trailing = if (url != DefaultServerUrl) {
                { IconButton(Icons.Retry, stringResource(Res.string.settings_server_default), { url = DefaultServerUrl }) }
            } else {
                null
            },
        )
    }
}
