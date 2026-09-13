package app.reseam.manager.ui.bundles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.Bundle
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BannerVariant
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.IconTile
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.home.patchCountLabel
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun BundlesScreen(viewModel: BundlesViewModel, selectedId: String?, onBack: (() -> Unit)?, onOpen: (Bundle) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    var adding by rememberSaveable { mutableStateOf(false) }
    Screen(
        title = "Bundles",
        onBack = onBack,
        actions = {
            if (state.busy) Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Spinner(size = 20) }
            IconButton(Icons.Plus, "Add bundle", onClick = { adding = true }, tint = colors.primary)
        },
    ) {
        item {
            Text(
                text = "Bundles are collections of patches. The official bundle updates itself; add more from a URL or a file.",
                style = ReseamTheme.typography.bodySmall,
                color = colors.mutedForeground,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        if (state.bundles.isEmpty()) {
            item {
                Banner(
                    message = if (state.busy) "Downloading the official patches" else "No bundles installed yet.",
                    variant = if (state.busy) BannerVariant.Progress else BannerVariant.Neutral,
                )
            }
        }
        items(state.bundles, key = { it.id }) { bundle ->
            BundleRow(
                bundle = bundle,
                selected = bundle.id == selectedId,
                onClick = { onOpen(bundle) },
                onAction = { if (bundle.official) viewModel.refreshOfficial() else viewModel.remove(bundle.id) },
                modifier = Modifier.animateItem(),
            )
        }
        item {
            Button(
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Large,
                icon = Icons.Plus,
            ) { Text("Add bundle") }
        }
    }
    if (adding) {
        AddBundleSheet(
            onDismiss = { adding = false },
            onUrl = { adding = false; viewModel.importUrl(it) },
            onFile = { adding = false; viewModel.importFile() },
        )
    }
    state.pendingTrust?.let { staged ->
        TrustBundleSheet(staged, onDecide = viewModel::decideTrust)
    }
}

@Composable
private fun BundleRow(bundle: Bundle, selected: Boolean, onClick: () -> Unit, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        background = if (selected) colors.primaryFaint else colors.surface,
        borderColor = if (selected) colors.primaryHairline else colors.border,
        onClick = onClick,
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(
                icon = Icons.Puzzle,
                background = if (bundle.official) colors.primary else colors.mutedElevated,
                tint = if (bundle.official) colors.onPrimary else colors.foreground,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(bundle.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Chip(if (bundle.official) "Official" else "Trusted", variant = if (bundle.official) ChipVariant.Primary else ChipVariant.Neutral)
                }
                if (bundle.description.isNotBlank()) {
                    Text(bundle.description, style = ReseamTheme.typography.caption, color = colors.mutedForeground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    text = listOfNotNull(patchCountLabel(bundle.patches.size), bundle.version, bundle.author.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = ReseamTheme.typography.monoSmall,
                    color = colors.subtleForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                icon = if (bundle.official) Icons.Refresh else Icons.Trash,
                contentDescription = if (bundle.official) "Check for updates" else "Remove",
                onClick = onAction,
                tint = colors.subtleForeground,
            )
        }
    }
}
