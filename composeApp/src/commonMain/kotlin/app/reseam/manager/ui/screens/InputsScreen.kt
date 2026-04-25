package app.reseam.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.RsAppIcon
import app.reseam.manager.ui.components.RsBottomBar
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsButtonVariant
import app.reseam.manager.ui.components.RsChip
import app.reseam.manager.ui.components.RsChipVariant
import app.reseam.manager.ui.components.RsIconButton
import app.reseam.manager.ui.components.RsSearchField
import app.reseam.manager.ui.components.RsSegment
import app.reseam.manager.ui.components.RsSegmentedControl
import app.reseam.manager.ui.components.RsStepLabel
import app.reseam.manager.ui.components.RsStepper
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.InputMode
import app.reseam.manager.ui.model.InstalledAppSummary
import app.reseam.manager.ui.model.PatchFlowState
import app.reseam.manager.ui.model.PatchInput
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun InputsScreen(
    state: PatchFlowState,
    bundles: List<BundleSummary>,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onBundles: () -> Unit,
    onSetMode: (InputMode) -> Unit,
    onSearch: (String) -> Unit,
    onSelectInstalled: (String) -> Unit,
    onPickFile: () -> Unit,
    onPickWebSource: (sourceUrl: String, name: String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RsTopBar(title = "New patch", onBack = onBack) {
            RsIconButton(onClick = onSettings, size = 36.dp, tint = colors.mutedForeground) {
                Icon(
                    imageVector = ReseamIcons.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                )
            }
        }
        RsStepper(current = 0, steps = listOf("Inputs", "Patches", "Run"))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
                    RsStepLabel(step = 1)
                    Text(
                        text = "Pick an app",
                        style = ReseamTheme.typography.display,
                        color = colors.foreground,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    )
                    Text(
                        text = "Choose what to patch. Your original app stays installed.",
                        style = ReseamTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                    )
                }
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    RsSegmentedControl(
                        selected = state.inputMode,
                        onSelect = onSetMode,
                        segments = listOf(
                            RsSegment(InputMode.Installed, "Installed", ReseamIcons.Smartphone),
                            RsSegment(InputMode.File, "File", ReseamIcons.Folder),
                            RsSegment(InputMode.Web, "Web", ReseamIcons.Globe),
                        ),
                    )
                }
            }
            when (state.inputMode) {
                InputMode.Installed -> {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                            RsSearchField(
                                value = state.searchQuery,
                                onValueChange = onSearch,
                                placeholder = "Search installed apps",
                                leading = ReseamIcons.Search,
                            )
                        }
                    }
                    items(state.filteredInstalledApps, key = { it.id }) { app ->
                        InstalledAppItem(
                            app = app,
                            selected = (state.selectedInput as? PatchInput.InstalledApp)?.app?.id == app.id,
                            onClick = { onSelectInstalled(app.id) },
                        )
                    }
                }
                InputMode.File -> {
                    item { FileDropzone(onPickFile = onPickFile) }
                }
                InputMode.Web -> {
                    item { WebSources(onPick = onPickWebSource) }
                }
            }
            item {
                BundlesStrip(bundles = bundles, onBundles = onBundles)
            }
        }
        RsBottomBar {
            val selectedName = state.selectedInput?.displayName
            RsButton(
                onClick = onContinue,
                size = RsButtonSize.Large,
                fullWidth = true,
                enabled = state.canContinueFromInputs,
            ) {
                Text(if (selectedName != null) "Continue with $selectedName" else "Pick an app")
                if (selectedName != null) {
                    Icon(
                        imageVector = ReseamIcons.ArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledAppItem(
    app: InstalledAppSummary,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = ReseamTheme.colors
    val hasPatches = app.hasCompatiblePatches
    val patchCount = app.compatiblePatchCount
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.primaryFaint else Color.Transparent)
            .border(
                1.dp,
                if (selected) colors.primaryHairline else Color.Transparent,
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = hasPatches, onClick = onClick)
            .alpha(if (hasPatches) 1f else 0.5f)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RsAppIcon(name = app.name, packageName = app.packageName, size = 44.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = app.name,
                style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.foreground,
            )
            val versionText = listOfNotNull(app.versionName, app.packageName)
                .firstOrNull() ?: app.packageName
            Text(
                text = versionText,
                style = ReseamTheme.typography.captionSmall.copy(fontFamily = ReseamTheme.typography.mono),
                color = colors.mutedForeground,
            )
        }
        if (hasPatches && patchCount != null && patchCount > 0) {
            val label = if (patchCount == 1) "1 patch" else "$patchCount patches"
            RsChip(
                text = label,
                variant = if (selected) RsChipVariant.SolidPrimary else RsChipVariant.Primary,
            )
        } else if (!hasPatches) {
            Text(
                text = "No patches",
                style = ReseamTheme.typography.captionSmall,
                color = colors.mutedForeground,
            )
        }
    }
}

@Composable
private fun FileDropzone(onPickFile: () -> Unit) {
    val colors = ReseamTheme.colors
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0E0E0E))
                .border(1.dp, colors.borderStrong, RoundedCornerShape(16.dp))
                .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primaryFaint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ReseamIcons.File,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                )
            }
            Text(
                text = "Choose an APK file",
                style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.foreground,
            )
            Text(
                text = "Pick a .apk or .apks split bundle from your device storage.",
                style = ReseamTheme.typography.caption,
                color = colors.mutedForeground,
            )
            Box(modifier = Modifier.padding(top = 8.dp)) {
                RsButton(
                    onClick = onPickFile,
                    variant = RsButtonVariant.Subtle,
                ) {
                    Icon(
                        imageVector = ReseamIcons.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                    )
                    Text("Browse files")
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardElevated)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = ReseamIcons.Info,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall).padding(top = 2.dp),
            )
            Text(
                text = "Already downloaded an APK from APKMirror or Aurora Store? Use this.",
                style = ReseamTheme.typography.caption,
                color = colors.mutedForeground,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private data class WebSourceEntry(val name: String, val host: String, val note: String, val url: String)

@Composable
private fun WebSources(onPick: (String, String) -> Unit) {
    val colors = ReseamTheme.colors
    val sources = listOf(
        WebSourceEntry("APKMirror", "apkmirror.com", "Verified uploads, most apps", "https://apkmirror.com"),
        WebSourceEntry("Aurora Store", "auroraoss.com", "Play Store bridge, anonymous", "https://auroraoss.com"),
        WebSourceEntry("F-Droid", "f-droid.org", "Open-source apps only", "https://f-droid.org"),
    )
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Fetch a clean APK directly. No Play Store needed.",
            style = ReseamTheme.typography.caption,
            color = colors.mutedForeground,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        sources.forEach { src ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .clickable { onPick(src.url, src.name) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(colors.mutedElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ReseamIcons.Download,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = src.name,
                        style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                        color = colors.foreground,
                    )
                    Text(
                        text = src.note,
                        style = ReseamTheme.typography.captionSmall,
                        color = colors.mutedForeground,
                    )
                }
                Icon(
                    imageVector = ReseamIcons.ExternalLink,
                    contentDescription = null,
                    tint = colors.mutedForeground,
                    modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                )
            }
        }
    }
}

@Composable
private fun BundlesStrip(bundles: List<BundleSummary>, onBundles: () -> Unit) {
    val colors = ReseamTheme.colors
    val active = bundles.firstOrNull { it.trusted } ?: bundles.firstOrNull() ?: return
    Box(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0E0E0E))
                .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                .clickable(onClick = onBundles)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = ReseamIcons.ShieldCheck,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    text = active.name,
                    style = ReseamTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.foreground,
                )
                Text(
                    text = if (active.trusted) " · trusted bundle" else " · untrusted",
                    style = ReseamTheme.typography.caption,
                    color = colors.mutedForeground,
                )
            }
            Icon(
                imageVector = ReseamIcons.ChevronRight,
                contentDescription = null,
                tint = colors.subtleForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
        }
    }
}
