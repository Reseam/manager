package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.resources.*
import app.reseam.manager.ui.byteSize
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.versionLabel
import org.jetbrains.compose.resources.stringResource

@Composable
fun SavedApksScreen(viewModel: SavedApksViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    SettingsPage(stringResource(Res.string.settings_saved), onBack) {
        itemGroup {
            item { shape ->
                SwitchItem(stringResource(Res.string.saved_keep), settings.keepDownloads, viewModel::setKeepDownloads, shape, supporting = stringResource(Res.string.saved_keep_supporting), icon = Icons.Download)
            }
        }
        if (rows.isEmpty()) {
            item { StateMessage(Icons.Package, stringResource(Res.string.saved_empty_title), stringResource(Res.string.saved_empty_body), Modifier.padding(top = Space.md)) }
        } else {
            itemGroup(top = Space.md) {
                rows.forEach { row ->
                    item(key = row.apk.id) { shape ->
                        val version = versionLabel(row.apk.versionName)
                        val size = byteSize(row.apk.sizeBytes)
                        GroupedItem(
                            title = row.apk.name,
                            supporting = stringResource(if (row.inUse) Res.string.saved_in_use else Res.string.saved_item, version, size),
                            shape = shape,
                            icon = Icons.Package,
                            trailing = { IconButton(Icons.Trash, stringResource(Res.string.saved_delete, row.apk.name), { viewModel.remove(row.apk) }) },
                        )
                    }
                }
            }
        }
    }
}
