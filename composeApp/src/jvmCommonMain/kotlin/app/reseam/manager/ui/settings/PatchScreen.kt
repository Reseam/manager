package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.resources.*
import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

@Composable
fun PatchScreen(viewModel: PatchViewModel, onBack: () -> Unit) {
    val appNames by viewModel.appNames.collectAsStateWithLifecycle()
    val patch = viewModel.patch.collectAsStateWithLifecycle().value ?: return
    SettingsPage(patch.spec.name, onBack) {
        if (patch.spec.description.isNotEmpty()) {
            item { Text(patch.spec.description, Modifier.padding(bottom = Space.md), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        itemGroup {
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.patch_default),
                    supporting = stringResource(if (patch.spec.enabledByDefault) Res.string.patch_default_on else Res.string.patch_default_off),
                    shape = shape,
                    icon = Icons.CircleCheck,
                )
            }
        }
        pageSection(Res.string.patch_works_with)
        itemGroup {
            if (patch.universal) {
                item { shape -> GroupedItem(stringResource(Res.string.patch_any_app), shape, supporting = stringResource(Res.string.patch_any_version), icon = Icons.Phone) }
            }
            patch.declared.forEach { target ->
                item { shape ->
                    GroupedItem(
                        title = appNames[target.`package`] ?: target.`package`,
                        supporting = target.versions.joinToString().ifEmpty { stringResource(Res.string.patch_any_version) },
                        shape = shape,
                        icon = Icons.Phone,
                    )
                }
            }
        }
        if (patch.spec.options.isNotEmpty()) {
            pageSection(Res.string.patch_options)
            itemGroup {
                patch.spec.options.forEach { option ->
                    item { shape -> GroupedItem(option.title, shape, supporting = option.description.ifEmpty { null }, icon = Icons.Settings) }
                }
            }
        }
    }
}
