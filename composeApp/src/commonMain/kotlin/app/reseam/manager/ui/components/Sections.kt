package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.padding(bottom = 20.dp)) {
        Text(
            text = title.uppercase(),
            style = ReseamTheme.typography.label,
            color = colors.mutedForeground,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .border(1.dp, colors.divider, RoundedCornerShape(14.dp)),
            content = content,
        )
    }
}

@Composable
fun RsSettingRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val colors = ReseamTheme.colors
    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(ReseamTheme.dimens.iconContainer)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.mutedElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.mutedForeground,
                    modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ReseamTheme.typography.body,
                    color = colors.foreground,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = ReseamTheme.typography.caption,
                        color = colors.mutedForeground,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = ReseamIcons.ChevronRight,
                    contentDescription = null,
                    tint = colors.subtleForeground,
                    modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider),
            )
        }
    }
}

@Composable
fun RsInfoLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    leadingTrust: Boolean = false,
) {
    val colors = ReseamTheme.colors
    Column {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = label.uppercase(),
                    style = ReseamTheme.typography.label,
                    color = colors.mutedForeground,
                    modifier = Modifier.widthIn(min = 96.dp),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    if (leadingTrust) {
                        Icon(
                            imageVector = ReseamIcons.ShieldCheck,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                        )
                    }
                    Text(
                        text = value,
                        style = ReseamTheme.typography.mono12,
                        color = colors.foreground,
                        textAlign = TextAlign.End,
                    )
                }
            }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider),
            )
        }
    }
}

@Composable
fun RsDetailLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = ReseamTheme.typography.label,
            color = colors.mutedForeground,
            modifier = Modifier.widthIn(min = 86.dp),
        )
        Text(
            text = value,
            style = ReseamTheme.typography.mono10.copy(fontSize = 12.sp),
            color = colors.foreground,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
