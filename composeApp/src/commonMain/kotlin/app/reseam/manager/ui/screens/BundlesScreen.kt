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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.RsBottomSheet
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsButtonVariant
import app.reseam.manager.ui.components.RsChip
import app.reseam.manager.ui.components.RsChipVariant
import app.reseam.manager.ui.components.RsIconButton
import app.reseam.manager.ui.components.RsInfoLine
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.components.RsValueField
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.PendingBundleTrust
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun BundlesScreen(
    bundles: List<BundleSummary>,
    pending: PendingBundleTrust?,
    onBack: () -> Unit,
    onImportFromUrl: (String) -> Unit,
    onImportFromFile: () -> Unit,
    onDecideTrust: (Boolean) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    var addSheet by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            RsTopBar(title = "Bundles", onBack = onBack) {
                RsIconButton(onClick = { addSheet = true }, size = 36.dp, tint = colors.primary) {
                    Icon(
                        imageVector = ReseamIcons.Plus,
                        contentDescription = "Add bundle",
                        modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
            ) {
                item {
                    Text(
                        text = "Bundles are collections of patches. The official bundle ships with Reseam; you can add more from a URL or a file.",
                        style = ReseamTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
                    )
                }
                items(bundles, key = { it.id }) { bundle ->
                    BundleRow(
                        bundle = bundle,
                        onRemove = { onRemove(bundle.id) },
                    )
                }
                item {
                    Box(modifier = Modifier.padding(16.dp)) {
                        AddBundleButton(onClick = { addSheet = true })
                    }
                }
            }
        }
        RsBottomSheet(
            visible = addSheet,
            onDismissRequest = { addSheet = false },
        ) {
            AddBundleSheetContent(
                onClose = { addSheet = false },
                onSubmitUrl = { url ->
                    addSheet = false
                    onImportFromUrl(url)
                },
                onSubmitFile = {
                    addSheet = false
                    onImportFromFile()
                },
            )
        }
        RsBottomSheet(
            visible = pending != null,
            onDismissRequest = { onDecideTrust(false) },
        ) {
            if (pending != null) {
                TrustBundleSheetContent(pending = pending, onDecide = onDecideTrust)
            }
        }
    }
}

@Composable
private fun BundleRow(
    bundle: BundleSummary,
    onRemove: () -> Unit,
) {
    val colors = ReseamTheme.colors
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (bundle.official) colors.primary else colors.mutedElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ReseamIcons.Puzzle,
                contentDescription = null,
                tint = if (bundle.official) Color.Black else colors.foreground,
                modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = bundle.name,
                    style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.foreground,
                )
                if (bundle.official) {
                    RsChip(text = "Official", variant = RsChipVariant.Primary)
                } else if (bundle.trusted) {
                    RsChip(text = "Trusted", variant = RsChipVariant.Default)
                }
            }
            Text(
                text = bundle.description,
                style = ReseamTheme.typography.captionSmall,
                color = colors.mutedForeground,
            )
            val meta = listOfNotNull(
                if (bundle.patchCount > 0) "${bundle.patchCount} patches" else null,
                bundle.version,
                bundle.updatedLabel,
            )
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" · "),
                    style = ReseamTheme.typography.captionSmall.copy(fontFamily = ReseamTheme.typography.mono),
                    color = colors.subtleForeground,
                )
            }
        }
        if (!bundle.official) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ReseamIcons.Trash,
                    contentDescription = "Remove",
                    tint = colors.subtleForeground,
                    modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                )
            }
        }
    }
}

@Composable
private fun AddBundleButton(onClick: () -> Unit) {
    val colors = ReseamTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, colors.borderStrong, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = ReseamIcons.Plus,
            contentDescription = null,
            tint = colors.mutedForeground,
            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
        )
        Text(
            text = "Add bundle",
            style = ReseamTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = colors.mutedForeground,
        )
    }
}

@Composable
private fun AddBundleSheetContent(
    onClose: () -> Unit,
    onSubmitUrl: (String) -> Unit,
    onSubmitFile: () -> Unit,
) {
    val colors = ReseamTheme.colors
    var url by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Add bundle",
            style = ReseamTheme.typography.title,
            color = colors.foreground,
        )
        Text(
            text = "Paste a bundle URL or pick a file from your device.",
            style = ReseamTheme.typography.caption,
            color = colors.mutedForeground,
        )
        RsValueField(
            value = url,
            onValueChange = { url = it },
            placeholder = "https://…",
            leading = ReseamIcons.Globe,
        )
        RsButton(
            onClick = { if (url.isNotBlank()) onSubmitUrl(url) },
            size = RsButtonSize.Large,
            fullWidth = true,
            enabled = url.isNotBlank(),
        ) {
            Icon(imageVector = ReseamIcons.Download, contentDescription = null, modifier = Modifier.size(ReseamTheme.dimens.iconSmall))
            Text("Import from URL")
        }
        RsButton(
            onClick = onSubmitFile,
            size = RsButtonSize.Large,
            fullWidth = true,
            variant = RsButtonVariant.Subtle,
        ) {
            Icon(imageVector = ReseamIcons.Folder, contentDescription = null, modifier = Modifier.size(ReseamTheme.dimens.iconSmall))
            Text("Pick from file")
        }
        RsButton(
            onClick = onClose,
            size = RsButtonSize.Medium,
            fullWidth = true,
            variant = RsButtonVariant.Ghost,
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun TrustBundleSheetContent(
    pending: PendingBundleTrust,
    onDecide: (Boolean) -> Unit,
) {
    val colors = ReseamTheme.colors
    val bundle = pending.bundle
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.mutedElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ReseamIcons.Puzzle,
                    contentDescription = null,
                    tint = colors.foreground,
                    modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = bundle.name,
                    style = ReseamTheme.typography.title,
                    color = colors.foreground,
                )
                if (bundle.source != null) {
                    Text(
                        text = bundle.source,
                        style = ReseamTheme.typography.captionSmall,
                        color = colors.mutedForeground,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.warningSoft)
                .border(1.dp, colors.warningHairline, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = ReseamIcons.TriangleAlert,
                contentDescription = null,
                tint = colors.warningForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
            Text(
                text = pending.warning,
                style = ReseamTheme.typography.caption,
                color = colors.mutedForeground,
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .border(1.dp, colors.divider, RoundedCornerShape(12.dp)),
        ) {
            RsInfoLine(label = "Patches", value = bundle.patchCount.toString())
            RsInfoLine(label = "Version", value = bundle.version ?: "—")
            RsInfoLine(
                label = "Signer",
                value = bundle.signerFingerprint ?: bundle.signerPublicKeyHex ?: "unsigned",
                showDivider = false,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RsButton(
                onClick = { onDecide(true) },
                size = RsButtonSize.Large,
                fullWidth = true,
            ) {
                Icon(
                    imageVector = ReseamIcons.ShieldCheck,
                    contentDescription = null,
                    modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                )
                Text("Trust & import")
            }
            RsButton(
                onClick = { onDecide(false) },
                size = RsButtonSize.Large,
                fullWidth = true,
                variant = RsButtonVariant.Ghost,
            ) {
                Text("Cancel")
            }
        }
    }
}
