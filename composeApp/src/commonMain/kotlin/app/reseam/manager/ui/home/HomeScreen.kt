package app.reseam.manager.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.Button
import androidx.compose.ui.platform.LocalUriHandler
import app.reseam.manager.ui.components.BannerVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.LogoMark
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.pressScale
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewPatch: () -> Unit,
    onOpenApp: (PatchedApp) -> Unit,
    onBundles: () -> Unit,
    onSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val uriHandler = LocalUriHandler.current
    Screen(
        title = "",
        header = { HomeHeader(onBundles, onSettings) },
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { HeroCard(onClick = onNewPatch, enabled = state.hasBundles) }
        state.update?.let { update ->
            item {
                Banner(
                    message = "Reseam Manager ${update.version} is available.",
                    variant = BannerVariant.Neutral,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    trailing = {
                        Button(onClick = { uriHandler.openUri(update.downloadUrl) }, size = ButtonSize.Small) { Text("Get") }
                    },
                )
            }
        }
        item {
            AnimatedVisibility(
                visible = state.syncing && !state.hasBundles,
                enter = fadeIn(motion.tweenBase()) + expandVertically(motion.tweenBase()),
                exit = fadeOut(motion.tweenFast()) + shrinkVertically(motion.tweenFast()),
            ) {
                Banner(
                    message = "Downloading the official patches. This happens once.",
                    variant = BannerVariant.Progress,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        if (state.patchedApps.isNotEmpty()) {
            item {
                SectionLabel(
                    text = "Your patched apps",
                    trailing = state.patchedApps.size.toString(),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 8.dp),
                )
            }
            items(state.patchedApps, key = { it.packageName }) { app ->
                PatchedAppRow(app, onClick = { onOpenApp(app) }, modifier = Modifier.animateItem())
            }
        }
        item {
            Text(
                text = "Everything runs on this device. Nothing leaves it.",
                style = ReseamTheme.typography.captionSmall,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun HomeHeader(onBundles: () -> Unit, onSettings: () -> Unit) {
    val colors = ReseamTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LogoMark(size = 26.dp)
        Text("Reseam", style = ReseamTheme.typography.title, color = colors.foreground, modifier = Modifier.weight(1f))
        IconButton(Icons.Puzzle, "Bundles", onBundles)
        IconButton(Icons.Settings, "Settings", onSettings)
    }
}

@Composable
private fun HeroCard(onClick: () -> Unit, enabled: Boolean) {
    val colors = ReseamTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .pressScale(interaction, pressedScale = 0.985f)
            .clip(ReseamTheme.shapes.hero)
            .background(Brush.linearGradient(listOf(colors.primary, colors.primaryBright)))
            .clickable(interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 24.dp),
    ) {
        Icon(
            imageVector = Icons.Sparkles,
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.16f),
            modifier = Modifier.align(Alignment.TopEnd).size(110.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Start here", style = ReseamTheme.typography.label, color = Color.Black.copy(alpha = 0.7f))
            Text("Patch an app", style = ReseamTheme.typography.headline, color = Color.Black)
            Text(
                text = "Pick one of your apps, choose features to add or remove, done.",
                style = ReseamTheme.typography.bodySmall,
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.widthIn(max = 240.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.15f)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Get started", style = ReseamTheme.typography.captionMedium, color = Color.Black)
                Icon(Icons.ArrowRight, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun PatchedAppRow(app: PatchedApp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
        background = colors.surfaceSunken,
        borderColor = colors.divider,
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(app.name, app.packageName, iconPath = app.iconPath)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.name, style = ReseamTheme.typography.titleSmall, color = colors.foreground)
                Text(
                    text = listOfNotNull(patchCountLabel(app.patches.size), app.versionName).joinToString(" · "),
                    style = ReseamTheme.typography.caption,
                    color = colors.mutedForeground,
                )
            }
            Icon(Icons.ChevronRight, null, tint = colors.subtleForeground, modifier = Modifier.size(20.dp))
        }
    }
}

fun patchCountLabel(count: Int): String = if (count == 1) "1 patch" else "$count patches"
