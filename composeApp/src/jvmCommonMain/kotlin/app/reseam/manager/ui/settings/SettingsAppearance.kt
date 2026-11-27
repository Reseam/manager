package app.reseam.manager.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import app.reseam.manager.ManagerLanguages
import app.reseam.manager.data.Settings
import app.reseam.manager.data.Theme
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
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
fun ThemeDialog(current: Theme, onChoose: (Theme) -> Unit, onDismiss: () -> Unit) =
    ChoiceDialog(stringResource(Res.string.settings_theme), Icons.SunMoon, Theme.entries, current, { it.label() }, onChoose, onDismiss)

@Composable
fun LanguageDialog(current: String?, onChoose: (String?) -> Unit, onDismiss: () -> Unit) =
    ChoiceDialog(stringResource(Res.string.settings_language), Icons.Languages, listOf(null) + ManagerLanguages, current, { languageLabel(it) }, onChoose, onDismiss)

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
