package app.reseam.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.RsAlertBanner
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsButtonVariant
import app.reseam.manager.ui.components.RsCard
import app.reseam.manager.ui.components.RsIconTile
import app.reseam.manager.ui.components.RsInfoLine
import app.reseam.manager.ui.components.RsValueField
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.PendingBundleTrust
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
internal fun AddBundleSheet(
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
            placeholder = "https://...",
            leading = ReseamIcons.Globe,
        )
        RsButton(
            onClick = { onSubmitUrl(url.trim()) },
            size = RsButtonSize.Large,
            fullWidth = true,
            enabled = url.isNotBlank(),
        ) {
            Icon(
                imageVector = ReseamIcons.Download,
                contentDescription = null,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
            Text("Import from URL")
        }
        RsButton(
            onClick = onSubmitFile,
            size = RsButtonSize.Large,
            fullWidth = true,
            variant = RsButtonVariant.Subtle,
        ) {
            Icon(
                imageVector = ReseamIcons.Folder,
                contentDescription = null,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
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
internal fun TrustBundleSheet(
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
            RsIconTile(
                icon = ReseamIcons.Puzzle,
                size = 44.dp,
            )
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
        RsAlertBanner(
            message = pending.warning,
            horizontalPadding = 12.dp,
            verticalPadding = 10.dp,
        ) {
            Icon(
                imageVector = ReseamIcons.TriangleAlert,
                contentDescription = null,
                tint = colors.warningForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
        }
        RsCard(
            modifier = Modifier.fillMaxWidth(),
            background = colors.background,
            cornerRadius = 12.dp,
        ) {
            Column {
                RsInfoLine(label = "Patches", value = bundle.patchCount.toString())
                RsInfoLine(label = "Version", value = bundle.version ?: "-")
                RsInfoLine(
                    label = "Signer",
                    value = bundle.signerFingerprint ?: bundle.signerPublicKeyHex ?: "unsigned",
                    showDivider = false,
                )
            }
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
