package app.reseam.manager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import app.reseam.manager.ui.components.RsCard
import app.reseam.manager.ui.components.RsChip
import app.reseam.manager.ui.components.RsChipVariant
import app.reseam.manager.ui.components.RsIconTile
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
internal fun BundleRow(
    bundle: BundleSummary,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    RsCard(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RsIconTile(
                icon = ReseamIcons.Puzzle,
                size = 42.dp,
                background = if (bundle.official) colors.primary else colors.mutedElevated,
                tint = if (bundle.official) colors.primaryForeground else colors.foreground,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BundleTitle(bundle)
                Text(
                    text = bundle.description,
                    style = ReseamTheme.typography.captionSmall,
                    color = colors.mutedForeground,
                )
                BundleMetadata(bundle)
            }
            BundleAction(
                official = bundle.official,
                onRefresh = onRefresh,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun BundleTitle(bundle: BundleSummary) {
    val colors = ReseamTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = bundle.name,
            style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
            color = colors.foreground,
        )
        when {
            bundle.official -> RsChip(text = "Official", variant = RsChipVariant.Primary)
            bundle.trusted -> RsChip(text = "Trusted", variant = RsChipVariant.Default)
        }
    }
}

@Composable
private fun BundleMetadata(bundle: BundleSummary) {
    val colors = ReseamTheme.colors
    val metadata = listOfNotNull(
        if (bundle.patchCount > 0) "${bundle.patchCount} patches" else null,
        bundle.version,
        bundle.updatedLabel,
    )
    if (metadata.isEmpty()) return

    Text(
        text = metadata.joinToString(" · "),
        style = ReseamTheme.typography.captionSmall.copy(fontFamily = ReseamTheme.typography.mono),
        color = colors.subtleForeground,
    )
}

@Composable
private fun BundleAction(
    official: Boolean,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = ReseamTheme.colors
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = if (official) onRefresh else onRemove),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (official) ReseamIcons.Refresh else ReseamIcons.Trash,
            contentDescription = if (official) "Check for updates" else "Remove",
            tint = colors.subtleForeground,
            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
        )
    }
}

@Composable
internal fun AddBundleButton(onClick: () -> Unit) {
    val colors = ReseamTheme.colors
    RsCard(
        modifier = Modifier.fillMaxWidth(),
        background = Color.Transparent,
        borderColor = colors.borderStrong,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
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
}
