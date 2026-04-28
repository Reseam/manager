package app.reseam.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

enum class RsAlertVariant { Warning, Neutral }

@Composable
fun RsAlertBanner(
    message: String,
    modifier: Modifier = Modifier,
    variant: RsAlertVariant = RsAlertVariant.Warning,
    horizontalPadding: Dp = 14.dp,
    verticalPadding: Dp = 12.dp,
    trailing: @Composable RowScope.() -> Unit = {},
    leading: @Composable RowScope.() -> Unit,
) {
    val colors = ReseamTheme.colors
    val (background, border) = when (variant) {
        RsAlertVariant.Warning -> colors.warningSoft to colors.warningHairline
        RsAlertVariant.Neutral -> colors.cardElevated to colors.border
    }
    RsCard(
        modifier = modifier.fillMaxWidth(),
        background = background,
        borderColor = border,
        cornerRadius = 12.dp,
        contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Text(
                text = message,
                style = ReseamTheme.typography.caption,
                color = colors.mutedForeground,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
    }
}
