package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.EngineVersion
import app.reseam.manager.ManagerVersion
import app.reseam.manager.data.DefaultApiBaseUrl
import app.reseam.manager.platform.Permission
import app.reseam.manager.platform.rememberPermissions
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.AboutCard
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.LocalPane
import app.reseam.manager.ui.nav.Pane
import app.reseam.manager.ui.nav.Route
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val LicenseUrl = "https://www.gnu.org/licenses/agpl-3.0.html"
private const val NoticeUrl = "https://git.reseam.app/reseam/manager/src/branch/main/NOTICE"
private const val SourceUrl = "https://git.reseam.app/reseam/manager"

private data class PermissionRow(val permission: Permission, val icon: ImageVector, val title: StringResource, val supporting: StringResource)

private val PermissionRows = listOf(
    PermissionRow(Permission.InstallApps, Icons.Download, Res.string.permission_install, Res.string.permission_install_supporting),
    PermissionRow(Permission.Notifications, Icons.Bell, Res.string.permission_notifications, Res.string.permission_notifications_supporting),
    PermissionRow(Permission.BatteryOptimization, Icons.Battery, Res.string.permission_background, Res.string.permission_background_supporting),
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val signingKey by viewModel.signingKey.collectAsStateWithLifecycle()
    val patchesVersion by viewModel.patchesVersion.collectAsStateWithLifecycle()
    val askingRoot by viewModel.askingRoot.collectAsStateWithLifecycle()
    val permissions = rememberPermissions()
    val uriHandler = LocalUriHandler.current
    val open = LocalDetailRoute.current
    val sourcesOpen = open is Route.Sources || open is Route.Source || (open == null && LocalPane.current == Pane.List)
    var editingServer by rememberSaveable { mutableStateOf(false) }
    var readingLicense by rememberSaveable { mutableStateOf(false) }

    SettingsPage(stringResource(Res.string.settings), onBack) {
        item {
            AboutCard(
                name = stringResource(Res.string.app_name),
                version = stringResource(Res.string.version, ManagerVersion),
                meta = patchesVersion?.let { stringResource(Res.string.about_meta, EngineVersion, it) } ?: stringResource(Res.string.about_meta_engine, EngineVersion),
            )
        }
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
                SwitchItem(stringResource(Res.string.settings_update), settings.autoUpdateBundles, viewModel::setAutoUpdateBundles, shape, supporting = stringResource(if (settings.autoUpdateBundles) Res.string.settings_update_supporting else Res.string.settings_update_supporting_off), icon = Icons.Refresh)
            }
            item { shape ->
                SwitchItem(stringResource(Res.string.settings_other_versions), settings.allowIncompatiblePatches, viewModel::setAllowIncompatiblePatches, shape, supporting = stringResource(Res.string.settings_other_versions_supporting), icon = Icons.Warning)
            }
        }
        pageSection(Res.string.settings_files)
        itemGroup {
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.settings_signing),
                    supporting = stringResource(if (signingKey == null) Res.string.settings_signing_none else Res.string.settings_signing_set),
                    shape = shape,
                    icon = Icons.Key,
                    selected = open is Route.SigningKey,
                    onClick = { onOpen(Route.SigningKey) },
                    trailing = { Chevron() },
                )
            }
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.settings_saved),
                    supporting = stringResource(Res.string.settings_saved_supporting),
                    shape = shape,
                    icon = Icons.Download,
                    selected = open is Route.SavedApks,
                    onClick = { onOpen(Route.SavedApks) },
                    trailing = { Chevron() },
                )
            }
        }
        if (permissions != null || viewModel.canMount) {
            pageSection(Res.string.settings_device)
            itemGroup {
                if (permissions != null) {
                    PermissionRows.forEach { row ->
                        item { shape ->
                            val granted = row.permission in permissions.granted
                            GroupedItem(
                                title = stringResource(row.title),
                                supporting = stringResource(row.supporting),
                                shape = shape,
                                icon = row.icon,
                                onClick = { permissions.request(row.permission) }.takeUnless { granted },
                                trailing = {
                                    if (granted) {
                                        Text(stringResource(Res.string.permission_allowed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Button(stringResource(Res.string.permission_allow), { permissions.request(row.permission) }, style = ButtonStyle.Tonal, size = ButtonSize.Medium)
                                    }
                                },
                            )
                        }
                    }
                }
                if (viewModel.canMount) {
                    item { shape ->
                        SwitchItem(stringResource(Res.string.settings_root), settings.mountWithRoot, viewModel::setMountWithRoot, shape, supporting = stringResource(Res.string.settings_root_supporting), icon = Icons.ShieldCheck, enabled = !askingRoot)
                    }
                }
            }
        }
        pageSection(Res.string.settings_advanced)
        itemGroup {
            item { shape ->
                GroupedItem(stringResource(Res.string.settings_server), shape, supporting = settings.apiBaseUrl, icon = Icons.Globe, onClick = { editingServer = true }, trailing = { Chevron() })
            }
            if (viewModel.installs) {
                item { shape ->
                    SwitchItem(stringResource(Res.string.settings_system_installer), settings.useSystemInstaller, viewModel::setUseSystemInstaller, shape, supporting = stringResource(Res.string.settings_system_installer_supporting), icon = Icons.Package)
                }
            }
        }
        pageSection(Res.string.settings_about)
        itemGroup {
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.settings_announcements),
                    supporting = stringResource(Res.string.settings_announcements_supporting),
                    shape = shape,
                    icon = Icons.Megaphone,
                    selected = open is Route.Announcements || open is Route.Announcement,
                    onClick = { onOpen(Route.Announcements) },
                    trailing = { Chevron() },
                )
            }
            item { shape ->
                GroupedItem(stringResource(Res.string.settings_license), shape, supporting = stringResource(Res.string.settings_license_supporting), icon = Icons.Scale, onClick = { readingLicense = true }, trailing = { Chevron() })
            }
            item { shape ->
                GroupedItem(stringResource(Res.string.settings_source_code), shape, supporting = SourceUrl.removePrefix("https://"), icon = Icons.Source, onClick = { uriHandler.openUri(SourceUrl) }, trailing = { Chevron() })
            }
        }
    }

    if (editingServer) {
        var url by rememberSaveable { mutableStateOf(settings.apiBaseUrl) }
        Dialog(
            headline = stringResource(Res.string.settings_server),
            body = stringResource(Res.string.settings_server_body),
            icon = Icons.Globe,
            onDismiss = { editingServer = false },
            actions = listOf(
                DialogAction(stringResource(Res.string.cancel), { editingServer = false }),
                DialogAction(stringResource(Res.string.save), {
                    viewModel.setApiBaseUrl(url)
                    editingServer = false
                }, enabled = url.isNotBlank()),
            ),
        ) {
            TextField(
                value = url,
                onValueChange = { url = it },
                label = stringResource(Res.string.settings_server_label),
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Uri,
                trailing = if (url != DefaultApiBaseUrl) {
                    { IconButton(Icons.Retry, stringResource(Res.string.settings_server_default), { url = DefaultApiBaseUrl }) }
                } else {
                    null
                },
            )
        }
    }
    if (readingLicense) {
        Sheet(stringResource(Res.string.settings_license), onDismiss = { readingLicense = false }) {
            Text(stringResource(Res.string.license_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(stringResource(Res.string.license_read), { uriHandler.openUri(LicenseUrl) }, Modifier.fillMaxWidth(), icon = Icons.ExternalLink)
            Button(stringResource(Res.string.license_notice), { uriHandler.openUri(NoticeUrl) }, Modifier.fillMaxWidth(), style = ButtonStyle.Tonal, icon = Icons.ExternalLink)
        }
    }
}
