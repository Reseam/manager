package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.name
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.Section
import app.reseam.manager.ui.components.SettingRow
import app.reseam.manager.ui.components.Toggle
import app.reseam.manager.ui.theme.ReseamTheme

private const val WebsiteUrl = "https://reseam.app"
private const val DocsUrl = "https://reseam.app/docs"
private const val SourceUrl = "https://git.reseam.app/reseam/manager"

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    versionLabel: String,
    onBack: (() -> Unit)?,
    onBundles: () -> Unit,
    onPermissions: (() -> Unit)?,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val signingKey by viewModel.signingKey.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    val uriHandler = LocalUriHandler.current
    var editingApi by rememberSaveable { mutableStateOf(false) }
    var signingOpen by rememberSaveable { mutableStateOf(false) }
    Screen(title = "Settings", onBack = onBack) {
        item {
            Section(
                title = "Patching",
                rows = buildList {
                    add { SettingRow("Bundles", Icons.Puzzle, subtitle = "Manage patch sources", onClick = onBundles) }
                    add {
                        SettingRow("Check for updates daily", Icons.Refresh, subtitle = "Official bundle only") {
                            Toggle(settings.checkUpdatesDaily, viewModel::setCheckUpdatesDaily, small = true)
                        }
                    }
                    if (onPermissions != null) {
                        add { SettingRow("Permissions", Icons.ShieldCheck, subtitle = "System grants Reseam needs", onClick = onPermissions) }
                    }
                },
            )
        }
        item {
            Section(
                title = "Advanced",
                rows = listOf(
                    {
                        SettingRow("Allow patches for other app versions", Icons.TriangleAlert, subtitle = "Run patches on versions they were not made for. They may fail or break the app.") {
                            Toggle(settings.allowIncompatiblePatches, viewModel::setAllowIncompatiblePatches, small = true)
                        }
                    },
                    {
                        SettingRow(
                            title = "Signing key",
                            icon = Icons.Key,
                            subtitle = signingKey?.fingerprint?.let { "SHA-256 ${it.take(23)}…" } ?: "Created on your first patch",
                            onClick = { signingOpen = true },
                        )
                    },
                    { SettingRow("API base URL", Icons.Globe, subtitle = settings.apiBaseUrl, onClick = { editingApi = true }) },
                ),
            )
        }
        item {
            Section(
                title = "About",
                rows = listOf(
                    { SettingRow("reseam.app", Icons.ExternalLink, subtitle = "Website", onClick = { uriHandler.openUri(WebsiteUrl) }) },
                    { SettingRow("Documentation", Icons.File, onClick = { uriHandler.openUri(DocsUrl) }) },
                    { SettingRow("Source code", Icons.GitBranch, onClick = { uriHandler.openUri(SourceUrl) }) },
                ),
            )
        }
        item {
            Text(
                text = versionLabel,
                style = ReseamTheme.typography.captionSmall,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )
        }
    }
    if (editingApi) {
        ApiBaseUrlSheet(
            initial = settings.apiBaseUrl,
            onDismiss = { editingApi = false },
            onSave = { viewModel.setApiBaseUrl(it); editingApi = false },
        )
    }
    if (signingOpen) {
        SigningKeySheet(
            info = signingKey,
            onDismiss = { signingOpen = false },
            onExport = viewModel::exportSigningKey,
            onImport = viewModel::pickKeystore,
            onReset = viewModel::resetSigningKey,
        )
    }
    pendingImport?.let { file ->
        PasswordSheet(
            title = "Keystore password",
            body = "Enter the password protecting ${file.name}. Its key replaces the current signing key; apps patched with the current key must be reinstalled.",
            confirmLabel = "Import",
            onDismiss = viewModel::cancelImport,
            onConfirm = viewModel::importSigningKey,
        )
    }
}
