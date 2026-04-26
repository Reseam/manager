package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RsCard(
    modifier: Modifier = Modifier,
    background: Color = ReseamTheme.colors.card,
    borderColor: Color? = ReseamTheme.colors.divider,
    cornerRadius: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .let { if (borderColor != null) it.border(1.dp, borderColor, shape) else it }
            .let { if (onClick != null) it.clickable(enabled = enabled, onClick = onClick) else it }
            .padding(contentPadding),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

@Composable
fun RsIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = 12.dp,
    background: Color = ReseamTheme.colors.mutedElevated,
    tint: Color = ReseamTheme.colors.foreground,
    iconSize: Dp = ReseamTheme.dimens.iconStandard,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}
