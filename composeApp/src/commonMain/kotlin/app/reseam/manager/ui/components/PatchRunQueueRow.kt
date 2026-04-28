package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.reseam.manager.patcher.PatchRunStatus
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun PatchRunQueueRow(
    name: String,
    status: PatchRunStatus?,
    active: Boolean,
    boxed: Boolean = false,
) {
    val colors = ReseamTheme.colors
    RsCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (boxed) 2.dp else 0.dp),
        background = when {
            boxed -> colors.surfaceInset
            active -> colors.cardElevated
            else -> Color.Transparent
        },
        borderColor = if (boxed) colors.divider else null,
        cornerRadius = 10.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PatchRunStatusDot(status = status, active = active)
            Text(
                text = name,
                style = ReseamTheme.typography.bodySmall,
                color = if (status != null || active) colors.foreground else colors.mutedForeground,
                modifier = Modifier.weight(1f),
            )
            when (status) {
                PatchRunStatus.Failed -> RsChip(text = "Failed", variant = RsChipVariant.Amber)
                PatchRunStatus.Skipped -> RsChip(text = "Skipped", variant = RsChipVariant.Default)
                else -> Unit
            }
        }
    }
}

@Composable
private fun PatchRunStatusDot(status: PatchRunStatus?, active: Boolean) {
    val colors = ReseamTheme.colors
    when {
        status == PatchRunStatus.Applied -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ReseamIcons.Check,
                contentDescription = null,
                tint = colors.primaryForeground,
                modifier = Modifier.size(14.dp),
            )
        }
        status == PatchRunStatus.Failed -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.warningSoft)
                .border(1.dp, colors.warningHairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ReseamIcons.TriangleAlert,
                contentDescription = null,
                tint = colors.warningForeground,
                modifier = Modifier.size(14.dp),
            )
        }
        status == PatchRunStatus.Skipped -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.muted)
                .border(1.dp, colors.divider, CircleShape),
        )
        active -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.primarySoft)
                .border(1.dp, colors.primaryHairline, CircleShape),
        )
        else -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.mutedElevated),
        )
    }
}
