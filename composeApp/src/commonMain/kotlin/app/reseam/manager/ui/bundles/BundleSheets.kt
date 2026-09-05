package app.reseam.manager.ui.bundles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reseam.manager.data.StagedBundle
import app.reseam.manager.data.TrustPrompt
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.IconTile
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.InfoCard
import app.reseam.manager.ui.components.InfoRow
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun AddBundleSheet(onDismiss: () -> Unit, onUrl: (String) -> Unit, onFile: () -> Unit) {
    val colors = ReseamTheme.colors
    var url by rememberSaveable { mutableStateOf("") }
    Sheet(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add bundle", style = ReseamTheme.typography.title, color = colors.foreground)
            Text("Paste a bundle URL or pick a .reseam file.", style = ReseamTheme.typography.caption, color = colors.mutedForeground)
            TextField(value = url, onValueChange = { url = it }, placeholder = "https://", leading = Icons.Globe)
            Button(onClick = { onUrl(url) }, size = ButtonSize.Large, fullWidth = true, enabled = url.isNotBlank()) {
                Icon(Icons.Download, null, modifier = Modifier.size(18.dp))
                Text("Import from URL")
            }
            Button(onClick = onFile, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Subtle) {
                Icon(Icons.Folder, null, modifier = Modifier.size(18.dp))
                Text("Pick a file")
            }
        }
    }
}

@Composable
fun TrustBundleSheet(staged: StagedBundle, onDecide: (Boolean) -> Unit) {
    val colors = ReseamTheme.colors
    val metadata = staged.metadata
    Sheet(onDismiss = { onDecide(false) }) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(Icons.Puzzle)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(metadata.name, style = ReseamTheme.typography.title, color = colors.foreground)
                    Text(staged.origin, style = ReseamTheme.typography.captionSmall, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val prompt = staged.prompt
            Banner(
                when (prompt) {
                    is TrustPrompt.ChangedApiSigner -> "The signer of your API's official bundle changed. Only continue if its operator announced a new key."
                    TrustPrompt.NewApiSigner -> "First bundle from your API. Check the signer against the key its operator publishes before continuing."
                    else -> "This bundle is signed by a key Reseam does not know. Trusted bundles run code that modifies apps. Only continue if you trust the source."
                },
            )
            InfoCard(
                rows = buildList {
                    add { InfoRow("Author", metadata.author.ifBlank { "unknown" }) }
                    add { InfoRow("Files", metadata.files.size.toString()) }
                    add { InfoRow("Signer", metadata.publicKey, mono = true) }
                    if (prompt is TrustPrompt.ChangedApiSigner) add { InfoRow("Previous signer", prompt.previous, mono = true) }
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onDecide(true) }, size = ButtonSize.Large, fullWidth = true) {
                    Icon(Icons.ShieldCheck, null, modifier = Modifier.size(18.dp))
                    Text("Trust and install")
                }
                Button(onClick = { onDecide(false) }, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Ghost) { Text("Cancel") }
            }
        }
    }
}
