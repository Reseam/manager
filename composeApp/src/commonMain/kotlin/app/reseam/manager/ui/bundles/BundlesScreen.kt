package app.reseam.manager.ui.bundles

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
fun BundlesScreen(viewModel: BundlesViewModel, onBack: () -> Unit, onOpen: (Bundle) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    var adding by rememberSaveable { mutableStateOf(false) }
    Screen(
        title = "Bundles",
        onBack = onBack,
        actions = {
            if (state.busy) Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { Spinner(size = 18) }
            IconButton(Icons.Plus, "Add bundle", onClick = { adding = true }, tint = colors.primary)
        },
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    ) {
        item {
            Text(
                text = "Bundles are collections of patches. The official bundle updates itself; add more from a URL or a file.",
                style = ReseamTheme.typography.bodySmall,
                color = colors.mutedForeground,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
            )
        }
        if (state.bundles.isEmpty()) {
            item {
                Banner(
                    message = if (state.busy) "Downloading the official patches" else "No bundles installed yet.",
                    variant = if (state.busy) BannerVariant.Progress else BannerVariant.Neutral,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        items(state.bundles, key = { it.id }) { bundle ->
            BundleRow(
                bundle = bundle,
                onClick = { onOpen(bundle) },
                onAction = { if (bundle.official) viewModel.refreshOfficial() else viewModel.remove(bundle.id) },
                modifier = Modifier.animateItem(),
            )
        }
        item {
            Button(
                onClick = { adding = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Large,
            ) {
                Icon(Icons.Plus, null, modifier = Modifier.size(18.dp))
                Text("Add bundle")
            }
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
private fun BundleRow(bundle: Bundle, onClick: () -> Unit, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
        onClick = onClick,
        contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(
                icon = Icons.Puzzle,
                size = 42.dp,
                background = if (bundle.official) colors.primary else colors.mutedElevated,
                tint = if (bundle.official) colors.onPrimary else colors.foreground,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(bundle.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Chip(if (bundle.official) "Official" else "Trusted", variant = if (bundle.official) ChipVariant.Primary else ChipVariant.Neutral)
                }
                Text(bundle.description, style = ReseamTheme.typography.captionSmall, color = colors.mutedForeground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOfNotNull(patchCountLabel(bundle.patches.size), bundle.version, bundle.author.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = ReseamTheme.typography.monoSmall,
                    color = colors.subtleForeground,
                )
            }
            IconButton(
                icon = if (bundle.official) Icons.Refresh else Icons.Trash,
                contentDescription = if (bundle.official) "Check for updates" else "Remove",
                onClick = onAction,
                size = 32.dp,
                tint = colors.subtleForeground,
            )
        }
    }
}
