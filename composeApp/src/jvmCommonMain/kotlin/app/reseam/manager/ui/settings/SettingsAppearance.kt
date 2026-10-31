package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reseam.manager.data.Settings
import app.reseam.manager.data.Theme
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.SelectedMark
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.theme.dynamicColorAvailable
import org.jetbrains.compose.resources.stringResource

/** Light or dark, and Material You where Android has it. */
fun LazyListScope.appearanceSection(viewModel: SettingsViewModel, settings: Settings, onChooseTheme: () -> Unit) {
    pageSection(Res.string.settings_appearance)
    itemGroup {
        item { shape ->
            GroupedItem(stringResource(Res.string.settings_theme), shape, supporting = settings.theme.label(), icon = Icons.SunMoon, onClick = onChooseTheme, trailing = { Chevron() })
        }
        if (dynamicColorAvailable) {
            item { shape ->
                SwitchItem(stringResource(Res.string.settings_dynamic_color), settings.dynamicColor, viewModel::setDynamicColor, shape, supporting = stringResource(Res.string.settings_dynamic_color_supporting), icon = Icons.Palette)
            }
        }
    }
}

@Composable
fun ThemeDialog(current: Theme, onChoose: (Theme) -> Unit, onDismiss: () -> Unit) {
    Dialog(
        headline = stringResource(Res.string.settings_theme),
        icon = Icons.SunMoon,
        onDismiss = onDismiss,
        actions = listOf(DialogAction(stringResource(Res.string.cancel), onDismiss)),
    ) {
        ItemGroup(Modifier.fillMaxWidth()) {
            Theme.entries.forEach { theme ->
                item { shape ->
                    GroupedItem(theme.label(), shape, onClick = { onChoose(theme) }, trailing = { if (theme == current) SelectedMark() })
                }
            }
        }
    }
}

@Composable
private fun Theme.label(): String = stringResource(
    when (this) {
        Theme.System -> Res.string.settings_theme_system
        Theme.Light -> Res.string.settings_theme_light
        Theme.Dark -> Res.string.settings_theme_dark
    },
)
