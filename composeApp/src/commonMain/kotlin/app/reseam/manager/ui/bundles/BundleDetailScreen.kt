package app.reseam.manager.ui.bundles

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
import app.reseam.manager.sdk.PatchMetadata
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.IconTile
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.InfoCard
import app.reseam.manager.ui.components.InfoRow
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun BundleDetailScreen(viewModel: BundleDetailViewModel, onBack: () -> Unit) {
    val bundle by viewModel.bundle.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    val current = bundle
    Screen(title = current?.name ?: "Bundle", onBack = onBack) {
        if (current == null) {
            item { EmptyState("Not found", "This bundle is no longer installed.") }
            return@Screen
        }
        item {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconTile(
                    icon = Icons.Puzzle,
                    size = 56.dp,
                    background = if (current.official) colors.primary else colors.mutedElevated,
                    tint = if (current.official) colors.onPrimary else colors.foreground,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(current.name, style = ReseamTheme.typography.title, color = colors.foreground)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(if (current.official) "Official" else "Trusted", variant = if (current.official) ChipVariant.Primary else ChipVariant.Neutral)
                        if (current.author.isNotBlank()) Chip(current.author)
                    }
                }
            }
        }
        if (current.description.isNotBlank()) {
            item {
                Text(current.description, style = ReseamTheme.typography.bodySmall, color = colors.mutedForeground, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                SectionLabel("Details", modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                InfoCard(
                    rows = listOfNotNull(
                        current.version?.let { { InfoRow("Version", it) } },
                        { InfoRow("Patches", current.patches.size.toString()) },
                        { InfoRow("Source", current.origin, mono = true) },
                        { InfoRow("Signer", current.id, mono = true) },
                    ),
                )
            }
        }
        item { SectionLabel("Patches", trailing = current.patches.size.toString(), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        items(current.patches, key = { it.id }) { PatchSummaryRow(it) }
        if (!current.official) {
            item {
                Button(
                    onClick = { viewModel.remove(onBack) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    variant = ButtonVariant.Danger,
                    size = ButtonSize.Large,
                ) {
                    Icon(Icons.Trash, null, modifier = Modifier.size(18.dp))
                    Text("Remove bundle")
                }
            }
        }
    }
}

@Composable
private fun PatchSummaryRow(patch: PatchMetadata) {
    val colors = ReseamTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(patch.id, style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
            if (patch.description.isNotBlank()) Text(patch.description, style = ReseamTheme.typography.captionSmall, color = colors.mutedForeground)
            val facts = listOfNotNull(
                patch.compatibility.takeIf { it.isNotEmpty() }?.joinToString { it.`package` },
                patch.options.takeIf { it.isNotEmpty() }?.let { "${it.size} options" },
                "default".takeIf { patch.enabledByDefault },
            )
            if (facts.isNotEmpty()) Text(facts.joinToString(" · "), style = ReseamTheme.typography.monoSmall, color = colors.subtleForeground)
        }
    }
}
