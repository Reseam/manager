package app.reseam.manager.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BannerVariant
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.CardPadding
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.LogoMark
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.paneGridColumns
import app.reseam.manager.ui.components.pressScale
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    selectedPackage: String?,
    showSectionActions: Boolean,
    onNewPatch: () -> Unit,
    onOpenApp: (PatchedApp) -> Unit,
    onBundles: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    val layout = ReseamTheme.layout
    val columns = paneGridColumns()
    val uriHandler = LocalUriHandler.current
    Screen(title = null, header = { HomeHeader(showSectionActions, onBundles, onSettings) }) {
        item { HeroCard(onClick = onNewPatch, enabled = state.hasBundles) }
        state.update?.let { update ->
            item {
                Banner(
                    message = "Reseam Manager ${update.version} is available.",
                    variant = BannerVariant.Neutral,
                    trailing = { Button(onClick = { uriHandler.openUri(update.downloadUrl) }, size = ButtonSize.Small) { Text("Get") } },
                )
            }
        }
        if (!state.hasBundles) {
            item {
                if (state.syncing) {
                    Banner("Downloading the official patches. This happens once.", variant = BannerVariant.Progress)
                } else {
                    EmptyState(
                        icon = Icons.Puzzle,
                        title = "No patches available",
                        body = "The official patch bundle could not be downloaded. Check your connection and try again, or add a bundle yourself.",
                        actions = {
                            Button(onClick = viewModel::retrySync, icon = Icons.Refresh) { Text("Try again") }
                            Button(onClick = onBundles, variant = ButtonVariant.Ghost) { Text("Bundles") }
                        },
                    )
                }
            }
        }
        if (state.patchedApps.isNotEmpty()) {
            item { SectionHeader("Your patched apps", trailing = state.patchedApps.size.toString()) }
            items(state.patchedApps.chunked(columns), key = { row -> row.joinToString { it.packageName } }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(layout.gutter), modifier = Modifier.animateItem()) {
                    row.forEach { app ->
                        PatchedAppRow(app, selected = app.packageName == selectedPackage, onClick = { onOpenApp(app) }, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (state.patchedApps.isEmpty()) {
            item {
                Text(
                    text = "Apps you patch will show up here.",
                    style = ReseamTheme.typography.captionSmall,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(showSectionActions: Boolean, onBundles: () -> Unit, onSettings: () -> Unit) {
    val colors = ReseamTheme.colors
    val layout = ReseamTheme.layout
    Box(Modifier.fillMaxWidth().padding(horizontal = layout.pageMargin - 8.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier.widthIn(max = layout.contentMaxWidth).fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LogoMark(size = 28.dp)
            Text("Reseam", style = ReseamTheme.typography.title, color = colors.foreground, modifier = Modifier.weight(1f))
            if (showSectionActions) {
                IconButton(Icons.Puzzle, "Bundles", onBundles)
                IconButton(Icons.Settings, "Settings", onSettings)
            }
        }
    }
}

@Composable
private fun HeroCard(onClick: () -> Unit, enabled: Boolean) {
    val colors = ReseamTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .pressScale(interaction, pressedScale = 0.985f)
            .clip(ReseamTheme.shapes.hero)
            .background(Brush.linearGradient(listOf(colors.primary, colors.primaryBright)))
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 26.dp),
    ) {
        Icon(
            imageVector = Icons.Sparkles,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.16f),
            modifier = Modifier.align(Alignment.TopEnd).size(120.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Start here", style = ReseamTheme.typography.label, color = Color.Black.copy(alpha = 0.7f))
            Text("Patch an app", style = ReseamTheme.typography.headline, color = Color.Black)
            Text(
                text = "Pick one of your apps, choose features to add or remove, done.",
                style = ReseamTheme.typography.bodySmall,
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.widthIn(max = 300.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.15f)).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Get started", style = ReseamTheme.typography.captionMedium, color = Color.Black)
                Icon(Icons.ArrowRight, null, tint = Color.Black, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PatchedAppRow(app: PatchedApp, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        background = if (selected) colors.primaryFaint else colors.surfaceSunken,
        borderColor = if (selected) colors.primaryHairline else colors.divider,
        onClick = onClick,
        contentPadding = CardPadding,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppIcon(app.name, app.packageName, iconPath = app.iconPath)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOfNotNull(patchCountLabel(app.patches.size), app.versionName).joinToString(" · "),
                    style = ReseamTheme.typography.caption,
                    color = colors.mutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.ChevronRight, null, tint = colors.subtleForeground, modifier = Modifier.size(20.dp))
        }
    }
}

fun patchCountLabel(count: Int): String = if (count == 1) "1 patch" else "$count patches"
