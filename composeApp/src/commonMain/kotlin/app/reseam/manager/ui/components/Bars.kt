package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = ReseamTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ReseamTheme.dimens.topBarHeight)
            .background(colors.background.copy(alpha = 0.92f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) {
                RsIconButton(onClick = onBack, size = 48.dp, tint = colors.foreground) {
                    Icon(
                        imageVector = ReseamIcons.ArrowLeft,
                        contentDescription = "Back",
                        modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                    )
                }
            }
        }
        Text(
            text = title,
            style = ReseamTheme.typography.title,
            color = colors.foreground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
            content = actions,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.divider),
    )
}

@Composable
fun RsBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = ReseamTheme.colors
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, colors.background),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
