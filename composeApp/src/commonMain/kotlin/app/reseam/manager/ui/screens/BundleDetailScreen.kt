package app.reseam.manager.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsButtonVariant
import app.reseam.manager.ui.components.RsCard
import app.reseam.manager.ui.components.RsChip
import app.reseam.manager.ui.components.RsChipVariant
import app.reseam.manager.ui.components.RsIconTile
import app.reseam.manager.ui.components.RsInfoLine
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.BundleDetailContent
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun BundleDetailScreen(
    detail: BundleDetailContent?,
    onBack: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    if (detail == null) {
        Column(modifier = modifier.fillMaxSize().background(colors.background)) {
            RsTopBar(title = "Bundle", onBack = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Bundle not found",
                    style = ReseamTheme.typography.body,
                    color = colors.mutedForeground,
                )
            }
        }
        return
    }

    val bundle = detail.bundle
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RsTopBar(title = bundle.name, onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RsIconTile(
                        icon = ReseamIcons.Puzzle,
                        size = 56.dp,
                        background = if (bundle.official) colors.primary else colors.mutedElevated,
                        tint = if (bundle.official) Color.Black else colors.foreground,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = bundle.name,
                            style = ReseamTheme.typography.title,
                            color = colors.foreground,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (bundle.official) RsChip(text = "Official", variant = RsChipVariant.Primary)
                            if (bundle.trusted) RsChip(text = "Trusted", variant = RsChipVariant.Default)
                            if (!bundle.trusted && !bundle.official) RsChip(text = "Untrusted", variant = RsChipVariant.Default)
                        }
                    }
                }
            }
            if (bundle.description.isNotBlank()) {
                item {
                    Text(
                        text = bundle.description,
                        style = ReseamTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "DETAILS",
                        style = ReseamTheme.typography.label,
                        color = colors.mutedForeground,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
                    )
                    RsCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            RsInfoLine(label = "Version", value = bundle.version ?: "—")
                            RsInfoLine(label = "Patches", value = detail.patches.size.toString())
                            if (bundle.source != null) {
                                RsInfoLine(label = "Source", value = bundle.source)
                            }
                            RsInfoLine(
                                label = "Signer",
                                value = bundle.signerFingerprint ?: "unsigned",
                                showDivider = bundle.signerPublicKeyHex != null,
                            )
                            if (bundle.signerPublicKeyHex != null) {
                                RsInfoLine(
                                    label = "Public key",
                                    value = bundle.signerPublicKeyHex.truncateMiddle(),
                                    showDivider = false,
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    text = "PATCHES (${detail.patches.size})",
                    style = ReseamTheme.typography.label,
                    color = colors.mutedForeground,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
                )
            }
            items(detail.patches, key = { it.name }) { patch ->
                PatchRow(patch)
            }
            if (!bundle.official) {
                item {
                    Box(modifier = Modifier.padding(16.dp)) {
                        RsButton(
                            onClick = { onRemove(bundle.id) },
                            size = RsButtonSize.Large,
                            fullWidth = true,
                            variant = RsButtonVariant.Ghost,
                        ) {
                            Icon(
                                imageVector = ReseamIcons.Trash,
                                contentDescription = null,
                                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                            )
                            Text("Remove bundle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatchRow(patch: PatchMetadata) {
    val colors = ReseamTheme.colors
    RsCard(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = patch.name,
                    style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.foreground,
                )
                if (patch.description.isNotBlank()) {
                    Text(
                        text = patch.description,
                        style = ReseamTheme.typography.captionSmall,
                        color = colors.mutedForeground,
                    )
                }
                val meta = listOfNotNull(
                    if (patch.options.isNotEmpty()) "${patch.options.size} options" else null,
                    if (patch.compatibleWith.isNotEmpty()) "${patch.compatibleWith.size} packages" else null,
                    if (patch.enabledByDefault) "default" else null,
                )
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        style = ReseamTheme.typography.captionSmall.copy(fontFamily = ReseamTheme.typography.mono),
                        color = colors.subtleForeground,
                    )
                }
            }
        }
    }
}

private fun String.truncateMiddle(prefix: Int = 10, suffix: Int = 8): String =
    if (length <= prefix + suffix + 1) this else "${take(prefix)}…${takeLast(suffix)}"
