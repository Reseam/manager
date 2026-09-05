package app.reseam.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

enum class BannerVariant { Warning, Neutral, Progress }

@Composable
fun Banner(
    message: String,
    modifier: Modifier = Modifier,
    variant: BannerVariant = BannerVariant.Warning,
    trailing: @Composable () -> Unit = {},
) {
    val colors = ReseamTheme.colors
    val warning = variant == BannerVariant.Warning
    Card(
        modifier = modifier.fillMaxWidth(),
        background = if (warning) colors.warningSoft else colors.surfaceSunken,
        borderColor = if (warning) colors.warningHairline else colors.divider,
        shape = ReseamTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (variant) {
                BannerVariant.Warning -> Icon(Icons.TriangleAlert, null, tint = colors.warningForeground, modifier = Modifier.size(18.dp))
                BannerVariant.Neutral -> Icon(Icons.Info, null, tint = colors.mutedForeground, modifier = Modifier.size(18.dp))
                BannerVariant.Progress -> CircularProgressIndicator(Modifier.size(18.dp), color = colors.primary, strokeWidth = 2.dp)
            }
            Text(
                text = message,
                style = ReseamTheme.typography.caption,
                color = if (warning) colors.warningForeground else colors.foreground,
                modifier = Modifier.weight(1f),
            )
            trailing()
        }
    }
}

@Composable
fun Spinner(modifier: Modifier = Modifier, size: Int = 20) {
    CircularProgressIndicator(modifier.size(size.dp), color = ReseamTheme.colors.primary, strokeWidth = 2.dp)
}
