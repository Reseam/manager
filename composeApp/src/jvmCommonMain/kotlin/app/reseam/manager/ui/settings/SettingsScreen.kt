package app.reseam.manager.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import app.reseam.manager.platform.rememberPermissions
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.LocalPane
import app.reseam.manager.ui.nav.Pane
import app.reseam.manager.ui.nav.Route
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onOpen: (Route) -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val signingKey by viewModel.signingKey.collectAsStateWithLifecycle()
    val patchesVersion by viewModel.patchesVersion.collectAsStateWithLifecycle()
    val askingRoot by viewModel.askingRoot.collectAsStateWithLifecycle()
    val permissions = rememberPermissions()
    val open = LocalDetailRoute.current
    val sourcesOpen = open is Route.Sources || open is Route.Source || (open == null && LocalPane.current == Pane.List)
    var editingServer by rememberSaveable { mutableStateOf(false) }
    var readingLicense by rememberSaveable { mutableStateOf(false) }
    var explainingPlayProtect by rememberSaveable { mutableStateOf(false) }
    var choosingLanguage by rememberSaveable { mutableStateOf(false) }
    var choosingTheme by rememberSaveable { mutableStateOf(false) }
    var asksBeforeUpdates by remember { mutableStateOf(viewModel.asksBeforeUpdates) }
    LifecycleResumeEffect(viewModel) {
        asksBeforeUpdates = viewModel.asksBeforeUpdates
        onPauseOrDispose {}
    }

    SettingsPage(stringResource(Res.string.settings), onBack) {
        aboutCard(patchesVersion)
        patchingSection(viewModel, settings, permissions, asksBeforeUpdates, sourcesOpen, onOpen) { on ->
            viewModel.setAutoUpdateApps(on)
            explainingPlayProtect = on && asksBeforeUpdates
        }
        filesSection(hasSigningKey = signingKey != null, open, onOpen)
        appearanceSection(viewModel, settings, onChooseLanguage = { choosingLanguage = true }, onChooseTheme = { choosingTheme = true })
        deviceSection(viewModel, settings, permissions, askingRoot)
        advancedSection(viewModel, settings, onEditServer = { editingServer = true })
        aboutSection(open, onOpen, onReadLicense = { readingLicense = true })
    }

    if (editingServer) {
        ServerDialog(
            current = settings.serverUrl,
            onSave = { url ->
                viewModel.setServerUrl(url)
                editingServer = false
            },
            onDismiss = { editingServer = false },
        )
    }
    if (choosingLanguage) {
        LanguageDialog(
            current = settings.language,
            onChoose = { language ->
                viewModel.setLanguage(language)
                choosingLanguage = false
            },
            onDismiss = { choosingLanguage = false },
        )
    }
    if (choosingTheme) {
        ThemeDialog(
            current = settings.theme,
            onChoose = { theme ->
                viewModel.setTheme(theme)
                choosingTheme = false
            },
            onDismiss = { choosingTheme = false },
        )
    }
    if (readingLicense) LicenseSheet(onDismiss = { readingLicense = false })
    if (explainingPlayProtect) PlayProtectDialog(onDismiss = { explainingPlayProtect = false })
}

/** The signing key and saved APKs. */
private fun LazyListScope.filesSection(hasSigningKey: Boolean, open: NavKey?, onOpen: (Route) -> Unit) {
    pageSection(Res.string.settings_files)
    itemGroup {
        item { shape ->
            GroupedItem(
                title = stringResource(Res.string.settings_signing),
                supporting = stringResource(if (hasSigningKey) Res.string.settings_signing_set else Res.string.settings_signing_none),
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
}
