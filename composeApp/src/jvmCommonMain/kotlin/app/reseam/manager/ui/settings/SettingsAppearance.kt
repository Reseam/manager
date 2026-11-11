package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reseam.manager.ManagerLanguages
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
import java.util.Locale
import org.jetbrains.compose.resources.stringResource

/** The language, light or dark, and Material You where Android has it. */
fun LazyListScope.appearanceSection(viewModel: SettingsViewModel, settings: Settings, onChooseLanguage: () -> Unit, onChooseTheme: () -> Unit) {
    pageSection(Res.string.settings_appearance)
    itemGroup {
        item { shape ->
            GroupedItem(stringResource(Res.string.settings_language), shape, supporting = languageLabel(settings.language), icon = Icons.Languages, onClick = onChooseLanguage, trailing = { Chevron() })
        }
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
fun LanguageDialog(current: String?, onChoose: (String?) -> Unit, onDismiss: () -> Unit) {
    Dialog(
        headline = stringResource(Res.string.settings_language),
        icon = Icons.Languages,
        onDismiss = onDismiss,
        actions = listOf(DialogAction(stringResource(Res.string.cancel), onDismiss)),
    ) {
        ItemGroup(Modifier.fillMaxWidth()) {
            (listOf(null) + ManagerLanguages).forEach { language ->
                item { shape ->
                    GroupedItem(languageLabel(language), shape, onClick = { onChoose(language) }, trailing = { if (language == current) SelectedMark() })
                }
            }
        }
    }
}

/** A language is named in itself, so it reads the same whatever the app's language is. */
@Composable
private fun languageLabel(language: String?): String {
    if (language == null) return stringResource(Res.string.settings_language_system)
    val locale = Locale.forLanguageTag(language)
    return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}

@Composable
private fun Theme.label(): String = stringResource(
    when (this) {
        Theme.System -> Res.string.settings_theme_system
        Theme.Light -> Res.string.settings_theme_light
        Theme.Dark -> Res.string.settings_theme_dark
    },
)
