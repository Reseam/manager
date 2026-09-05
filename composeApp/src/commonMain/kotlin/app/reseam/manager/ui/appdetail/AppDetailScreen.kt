package app.reseam.manager.ui.appdetail

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.InfoCard
import app.reseam.manager.ui.components.InfoRow
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onBack: () -> Unit,
    onRepatch: (PatchTarget) -> Unit,
) {
    val app by viewModel.app.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    val current = app
    Screen(
        title = current?.name ?: "App",
        onBack = onBack,
        actions = {
            if (current != null) Button(onClick = { viewModel.remove(onBack) }, variant = ButtonVariant.Danger, size = ButtonSize.Small, modifier = Modifier.padding(end = 8.dp)) { Text("Remove") }
        },
        bottomBar = if (current == null) null else {
            {
                BottomBar {
                    Button(onClick = viewModel::openArtifact, modifier = Modifier.weight(1f), variant = ButtonVariant.Ghost, size = ButtonSize.Large) {
                        Text(viewModel.artifactActionLabel)
                    }
                    Button(onClick = { onRepatch(current.target()) }, modifier = Modifier.weight(1f), size = ButtonSize.Large) {
                        Icon(Icons.Refresh, null, modifier = Modifier.size(18.dp))
                        Text("Re-patch")
                    }
                }
            }
        },
    ) {
        if (current == null) {
            item { EmptyState("Not found", "This patched app is no longer in the library.") }
            return@Screen
        }
        item {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppIcon(current.name, current.packageName, size = 56.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(current.name, style = ReseamTheme.typography.title, color = colors.foreground)
                    Text(current.versionName ?: current.packageName, style = ReseamTheme.typography.monoSmall, color = colors.mutedForeground)
                }
            }
        }
        item { SectionLabel("Patches applied", trailing = current.patches.size.toString(), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        items(current.patches, key = { it.id }) { patch ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                shape = ReseamTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Check, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(patch.id, style = ReseamTheme.typography.bodySmall, color = colors.foreground)
                        if (patch.bundle.isNotEmpty()) Text(patch.bundle, style = ReseamTheme.typography.monoSmall, color = colors.mutedForeground)
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                SectionLabel("Details", modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                InfoCard(
                    rows = listOf(
                        { InfoRow("Package", current.packageName, mono = true) },
                        { InfoRow("Version", current.versionName ?: "unknown") },
                        { InfoRow("File", current.apkPath.substringAfterLast('/'), mono = true) },
                    ),
                )
            }
        }
    }
}

private fun PatchedApp.target() = PatchTarget(name, packageName, versionName, apkPath)
