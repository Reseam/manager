package app.reseam.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.RsAppIcon
import app.reseam.manager.ui.components.RsBottomBar
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsButtonVariant
import app.reseam.manager.ui.components.RsIconButton
import app.reseam.manager.ui.components.RsInfoLine
import app.reseam.manager.ui.components.RsSection
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun AppDetailScreen(
    app: PatchedAppSummary?,
    onBack: () -> Unit,
    onRepatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    if (app == null) {
        Column(modifier = modifier.fillMaxSize().background(colors.background)) {
            RsTopBar(title = "App", onBack = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "App not found",
                    style = ReseamTheme.typography.body,
                    color = colors.mutedForeground,
                )
            }
        }
        return
    }
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RsTopBar(title = app.name, onBack = onBack) {
            RsIconButton(onClick = {}, size = 36.dp, tint = colors.mutedForeground) {
                Icon(
                    imageVector = ReseamIcons.MoreVertical,
                    contentDescription = "More",
                    modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RsAppIcon(name = app.name, packageName = app.packageName, size = 56.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = app.name,
                            style = ReseamTheme.typography.title,
                            color = colors.foreground,
                        )
                        Text(
                            text = app.versionName ?: "—",
                            style = ReseamTheme.typography.captionSmall.copy(fontFamily = ReseamTheme.typography.mono),
                            color = colors.mutedForeground,
                        )
                    }
                }
            }
            if (app.update != null) {
                item {
                    Row(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.primaryFaint)
                            .border(1.dp, colors.primaryHairline, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = ReseamIcons.Refresh,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Update to ${app.update.versionName}",
                                style = ReseamTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = colors.foreground,
                            )
                            if (app.update.compatible) {
                                Text(
                                    text = "Patches compatible",
                                    style = ReseamTheme.typography.captionSmall,
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
            item {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
                    SectionHeader(title = "Patches used")
                }
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(app.patchCount.coerceAtLeast(1)) { i ->
                        val patchName = "Patch ${i + 1}"
                        val bundleName = app.bundleNames.firstOrNull() ?: "—"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.card)
                                .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = ReseamIcons.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = patchName,
                                    style = ReseamTheme.typography.bodySmall,
                                    color = colors.foreground,
                                )
                                Text(
                                    text = bundleName,
                                    style = ReseamTheme.typography.captionSmall.copy(fontFamily = ReseamTheme.typography.mono),
                                    color = colors.mutedForeground,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "APP",
                        style = ReseamTheme.typography.label,
                        color = colors.mutedForeground,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.card)
                            .border(1.dp, colors.divider, RoundedCornerShape(14.dp)),
                    ) {
                        RsInfoLine(label = "Package", value = app.packageName)
                        RsInfoLine(label = "Version", value = app.versionName ?: "—")
                        RsInfoLine(label = "Artifact", value = app.artifactPath.substringAfterLast('/'), showDivider = false)
                    }
                }
            }
            if (app.bundleNames.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            text = "BUNDLE",
                            style = ReseamTheme.typography.label,
                            color = colors.mutedForeground,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.card)
                                .border(1.dp, colors.divider, RoundedCornerShape(14.dp)),
                        ) {
                            app.bundleNames.forEachIndexed { i, name ->
                                RsInfoLine(
                                    label = "Name",
                                    value = name,
                                    showDivider = i < app.bundleNames.size - 1,
                                    leadingTrust = i == 0,
                                )
                            }
                        }
                    }
                }
            }
        }
        RsBottomBar {
            RsButton(
                onClick = {},
                size = RsButtonSize.Large,
                fullWidth = true,
                variant = RsButtonVariant.Ghost,
                modifier = Modifier.weight(1f),
            ) {
                Text("Uninstall")
            }
            RsButton(
                onClick = onRepatch,
                size = RsButtonSize.Large,
                fullWidth = true,
                modifier = Modifier.weight(1f),
            ) {
                Text("Re-patch")
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = ReseamTheme.colors
    Text(
        text = title.uppercase(),
        style = ReseamTheme.typography.label,
        color = colors.mutedForeground,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}
