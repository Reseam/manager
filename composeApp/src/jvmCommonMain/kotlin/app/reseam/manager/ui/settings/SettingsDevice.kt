package app.reseam.manager.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import app.reseam.manager.data.Settings
import app.reseam.manager.platform.Permission
import app.reseam.manager.platform.Permissions
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.components.itemGroup
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class PermissionRow(val permission: Permission, val icon: ImageVector, val title: StringResource, val supporting: StringResource)

private val PermissionRows = listOf(
    PermissionRow(Permission.InstallApps, Icons.Download, Res.string.permission_install, Res.string.permission_install_supporting),
    PermissionRow(Permission.Notifications, Icons.Bell, Res.string.permission_notifications, Res.string.permission_notifications_supporting),
    PermissionRow(Permission.BatteryOptimization, Icons.Battery, Res.string.permission_background, Res.string.permission_background_supporting),
)

/** What Manager may do on the device: its permissions, and root, where the platform has them. */
fun LazyListScope.deviceSection(viewModel: SettingsViewModel, settings: Settings, permissions: Permissions?, askingRoot: Boolean) {
    if (permissions == null && !viewModel.canMount) return
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
