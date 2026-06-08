package app.reseam.manager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.RsAppIcon
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsCard
import app.reseam.manager.ui.components.RsIconButton
import app.reseam.manager.ui.components.RsLogoMark
import app.reseam.manager.ui.components.rsPressScale
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.HomeState
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun HomeScreen(
    state: HomeState,
    setupInProgress: Boolean,
    onNewPatch: () -> Unit,
    onOpenApp: (appId: String) -> Unit,
    onRepatch: (appId: String) -> Unit,
    onSettings: () -> Unit,
    onBundles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RsLogoMark(size = 26.dp)
            Text(
                text = "Reseam",
                style = ReseamTheme.typography.title,
                color = colors.foreground,
                modifier = Modifier.weight(1f),
            )
            RsIconButton(onClick = onBundles, size = 36.dp, tint = colors.mutedForeground) {
                Icon(
                    imageVector = ReseamIcons.Puzzle,
                    contentDescription = "Bundles",
                    modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                )
            }
            RsIconButton(onClick = onSettings, size = 36.dp, tint = colors.mutedForeground) {
                Icon(
                    imageVector = ReseamIcons.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item { HeroCard(onClick = onNewPatch) }

            item {
                AnimatedVisibility(
                    visible = setupInProgress,
                    enter = fadeIn(animationSpec = tween(ReseamTheme.motion.durationBase, easing = ReseamTheme.motion.easeOut)) +
                        expandVertically(animationSpec = tween(ReseamTheme.motion.durationBase, easing = ReseamTheme.motion.easeOut)),
                    exit = fadeOut(animationSpec = tween(ReseamTheme.motion.durationFast, easing = ReseamTheme.motion.easeOut)) +
                        shrinkVertically(animationSpec = tween(ReseamTheme.motion.durationFast, easing = ReseamTheme.motion.easeOut)),
                ) {
                    SetupInProgressCard()
                }
            }

            if (state.patchedApps.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = "YOUR PATCHED APPS",
                            style = ReseamTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                            color = colors.mutedForeground,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = state.patchedApps.size.toString(),
                            style = ReseamTheme.typography.captionSmall,
                            color = colors.mutedForeground,
                        )
                    }
                }
                items(state.patchedApps, key = { it.id }) { app ->
                    PatchedAppRow(
                        app = app,
                        onOpen = { onOpenApp(app.id) },
                        onRepatch = { onRepatch(app.id) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(ReseamTheme.motion.durationBase, easing = ReseamTheme.motion.easeOut),
                            placementSpec = tween(ReseamTheme.motion.durationBase, easing = ReseamTheme.motion.easeOut),
                            fadeOutSpec = tween(ReseamTheme.motion.durationFast, easing = ReseamTheme.motion.easeOut),
                        ),
                    )
                }
            }

            item {
                Text(
                    text = "Everything runs on your phone. Nothing leaves the device.",
                    style = ReseamTheme.typography.captionSmall,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SetupInProgressCard() {
    val colors = ReseamTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceSunken)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.5.dp,
            color = colors.primary,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Downloading official patches…",
                style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.foreground,
            )
            Text(
                text = "One moment — this happens once on first launch.",
                style = ReseamTheme.typography.caption,
                color = colors.mutedForeground,
            )
        }
    }
}

@Composable
private fun HeroCard(onClick: () -> Unit) {
    val colors = ReseamTheme.colors
    val gradient = Brush.linearGradient(
        colors = listOf(colors.primary, colors.primaryBright),
    )
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            .rsPressScale(interactionSource, pressedScale = 0.985f)
            .clip(RoundedCornerShape(22.dp))
            .background(gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(110.dp),
        ) {
            Icon(
                imageVector = ReseamIcons.Sparkles,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "START HERE",
                style = ReseamTheme.typography.label.copy(fontWeight = FontWeight.Bold),
                color = Color.Black.copy(alpha = 0.7f),
            )
            Text(
                text = "Patch an app",
                style = ReseamTheme.typography.headline.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black,
            )
            Text(
                text = "Pick one of your apps, choose features to add or remove, done.",
                style = ReseamTheme.typography.bodySmall,
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.widthIn(max = 240.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Get started",
                    style = ReseamTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = Color.Black,
                )
                Icon(
                    imageVector = ReseamIcons.ArrowRight,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                )
            }
        }
    }
}

@Composable
private fun PatchedAppRow(
    app: PatchedAppSummary,
    onOpen: () -> Unit,
    onRepatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val motion = ReseamTheme.motion
    val updateAvailable = app.update != null
    val targetBg = if (updateAvailable) colors.primaryFaint else colors.surfaceInset
    val targetBorder = if (updateAvailable) colors.primarySoft else colors.divider
    RsCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp),
        background = targetBg,
        borderColor = targetBorder,
        onClick = onOpen,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RsAppIcon(name = app.name, packageName = app.packageName, size = 44.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = app.name,
                        style = ReseamTheme.typography.titleSmall,
                        color = colors.foreground,
                    )
                    val patchLabel = if (app.patchCount == 1) "1 patch" else "${app.patchCount} patches"
                    val versionSuffix = app.versionName?.let { " · $it" } ?: ""
                    Text(
                        text = "$patchLabel$versionSuffix",
                        style = ReseamTheme.typography.caption,
                        color = colors.mutedForeground,
                    )
                }
                Icon(
                    imageVector = ReseamIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.subtleForeground,
                    modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                )
            }
            AnimatedVisibility(
                visible = app.update != null,
                enter = fadeIn(animationSpec = tween(motion.durationBase, easing = motion.easeOut)) +
                    expandVertically(animationSpec = tween(motion.durationBase, easing = motion.easeOut)),
                exit = fadeOut(animationSpec = tween(motion.durationFast, easing = motion.easeOut)) +
                    shrinkVertically(animationSpec = tween(motion.durationFast, easing = motion.easeOut)),
            ) {
                val update = app.update
                if (update != null) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.primaryHairline),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.primaryFaint)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .heightIn(min = 32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = ReseamIcons.Refresh,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                            )
                            Row(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update to ",
                                    style = ReseamTheme.typography.caption,
                                    color = colors.mutedForeground,
                                )
                                Text(
                                    text = update.versionName,
                                    style = ReseamTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                                    color = colors.foreground,
                                )
                                if (update.compatible) {
                                    Text(
                                        text = " · patches compatible",
                                        style = ReseamTheme.typography.caption,
                                        color = colors.mutedForeground,
                                    )
                                }
                            }
                            RsButton(onClick = onRepatch, size = RsButtonSize.Small) {
                                Text("Re-patch")
                            }
                        }
                    }
                }
            }
        }
    }
}
