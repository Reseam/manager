package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import app.reseam.manager.data.DefaultApiBaseUrl
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.Section
import app.reseam.manager.ui.components.SettingRow
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.components.Toggle
import app.reseam.manager.ui.theme.ReseamTheme

private const val WebsiteUrl = "https://reseam.app"
private const val DocsUrl = "https://reseam.app/docs"
private const val SourceUrl = "https://git.reseam.app/reseam/manager"

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    versionLabel: String,
    onBack: () -> Unit,
    onBundles: () -> Unit,
    onPermissions: (() -> Unit)?,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    val uriHandler = LocalUriHandler.current
    var editingApi by rememberSaveable { mutableStateOf(false) }
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
                    add { SettingRow("API base URL", Icons.Globe, subtitle = settings.apiBaseUrl, onClick = { editingApi = true }) }
                    if (onPermissions != null) {
                        add { SettingRow("Permissions", Icons.ShieldCheck, subtitle = "System grants Reseam needs", onClick = onPermissions) }
                    }
                },
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
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
}

@Composable
private fun ApiBaseUrlSheet(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val colors = ReseamTheme.colors
    var draft by rememberSaveable { mutableStateOf(initial) }
    Sheet(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("API base URL", style = ReseamTheme.typography.title, color = colors.foreground)
            Text(
                text = "Where the official patch index is fetched from. Change this only for self-hosted or staging servers.",
                style = ReseamTheme.typography.caption,
                color = colors.mutedForeground,
            )
            TextField(value = draft, onValueChange = { draft = it }, placeholder = DefaultApiBaseUrl, leading = Icons.Globe, mono = true)
            Button(onClick = { onSave(draft) }, size = ButtonSize.Large, fullWidth = true, enabled = draft.isNotBlank() && draft.trim() != initial) { Text("Save") }
            Button(onClick = { draft = DefaultApiBaseUrl }, size = ButtonSize.Medium, fullWidth = true, variant = ButtonVariant.Ghost, enabled = draft != DefaultApiBaseUrl) { Text("Reset to default") }
        }
    }
}
