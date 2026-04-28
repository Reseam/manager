package app.reseam.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.PatchFlowIntro
import app.reseam.manager.ui.components.PatchFlowScaffold
import app.reseam.manager.ui.components.RsAlertBanner
import app.reseam.manager.ui.components.RsAlertVariant
import app.reseam.manager.ui.components.RsAppIcon
import app.reseam.manager.ui.components.RsBottomBar
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsButtonVariant
import app.reseam.manager.ui.components.RsCard
import app.reseam.manager.ui.components.RsChip
import app.reseam.manager.ui.components.RsChipVariant
import app.reseam.manager.ui.components.RsIconButton
import app.reseam.manager.ui.components.RsIconTile
import app.reseam.manager.ui.components.RsSearchField
import app.reseam.manager.ui.components.RsSegment
import app.reseam.manager.ui.components.RsSegmentedControl
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.InputMode
import app.reseam.manager.ui.model.InstalledAppSummary
import app.reseam.manager.ui.model.LoadState
import app.reseam.manager.ui.model.PatchFlowState
import app.reseam.manager.ui.model.PatchInput
import app.reseam.manager.ui.theme.ReseamTheme

private val InputModeSegments = listOf(
    RsSegment(InputMode.Installed, "Installed", ReseamIcons.Smartphone),
    RsSegment(InputMode.File, "File", ReseamIcons.Folder),
)

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
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val inspectState = state.inspect
    val isInspecting = inspectState is LoadState.Loading
    PatchFlowScaffold(
        title = "New patch",
        currentStep = 0,
        modifier = modifier,
        onBack = onBack,
        actions = {
            RsIconButton(onClick = onSettings, size = 36.dp, tint = colors.mutedForeground) {
                Icon(
                    imageVector = ReseamIcons.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                )
            }
        },
        bottomBar = {
            RsBottomBar {
                val selectedName = state.selectedInput?.displayName
                RsButton(
                    onClick = onContinue,
                    size = RsButtonSize.Large,
                    fullWidth = true,
                    enabled = state.canContinueFromInputs,
                ) {
                    if (isInspecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                            strokeWidth = 2.dp,
                            color = colors.primaryForeground,
                        )
                        Text("Checking app...")
                    } else {
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
        },
    ) {
            item {
                PatchFlowIntro(
                    step = 1,
                    title = "Pick an app",
                    body = "Choose what to patch. Your original app stays installed.",
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                )
            }
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    RsSegmentedControl(
                        selected = state.inputMode,
                        onSelect = onSetMode,
                        segments = InputModeSegments,
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
            }
            when (inspectState) {
                LoadState.Loading -> item {
                    RsAlertBanner(
                        message = "Checking this app and loading compatible patches...",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        variant = RsAlertVariant.Neutral,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                            strokeWidth = 2.dp,
                            color = colors.primary,
                        )
                    }
                }
                is LoadState.Failed -> item {
                    RsAlertBanner(
                        message = inspectState.message,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = ReseamIcons.TriangleAlert,
                            contentDescription = null,
                            tint = colors.warningForeground,
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                        )
                    }
                }
                else -> Unit
            }
            item {
                BundlesStrip(bundles = bundles, onBundles = onBundles)
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
    RsCard(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .fillMaxWidth()
            .alpha(if (hasPatches) 1f else 0.5f),
        background = if (selected) colors.primaryFaint else Color.Transparent,
        borderColor = if (selected) colors.primaryHairline else Color.Transparent,
        cornerRadius = 14.dp,
        onClick = onClick,
        enabled = hasPatches,
        contentPadding = PaddingValues(10.dp),
    ) {
        Row(
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
                Text(
                    text = app.versionName ?: app.packageName,
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
        RsCard(
            modifier = Modifier.fillMaxWidth(),
            background = colors.surfaceSunken,
            borderColor = colors.borderStrong,
            cornerRadius = 16.dp,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 32.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RsIconTile(
                    icon = ReseamIcons.File,
                    size = 48.dp,
                    cornerRadius = 14.dp,
                    background = colors.primaryFaint,
                    tint = colors.primary,
                )
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
        }
        RsCard(
            modifier = Modifier.fillMaxWidth(),
            background = colors.cardElevated,
            borderColor = colors.border,
            cornerRadius = 12.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
}

@Composable
private fun BundlesStrip(bundles: List<BundleSummary>, onBundles: () -> Unit) {
    val colors = ReseamTheme.colors
    val active = bundles.firstOrNull { it.trusted } ?: bundles.firstOrNull() ?: return
    Box(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
        RsCard(
            modifier = Modifier.fillMaxWidth(),
            background = colors.surfaceSunken,
            borderColor = colors.divider,
            cornerRadius = 12.dp,
            onClick = onBundles,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
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
}
