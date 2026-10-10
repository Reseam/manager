package app.reseam.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.Borders
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space

@Composable
fun AppCard(app: AppLook, supporting: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 80.dp),
        shape = if (selected) RoundedCornerShape(Radius.sm) else CircleShape,
        color = if (selected) colors.secondaryContainer else colors.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = Space.lg, end = Space.xxl, top = Space.lg, bottom = Space.lg),
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app, Sizes.appIconMd)
            ItemText(app.name, supporting, Modifier.weight(1f))
        }
    }
}

@Composable
fun Chip(label: String, modifier: Modifier = Modifier, outlined: Boolean = false, onClick: (() -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.sm)
    val container = if (outlined) Color.Transparent else colors.secondaryContainer
    val content = if (outlined) colors.onSurfaceVariant else colors.onSecondaryContainer
    val border = if (outlined) BorderStroke(Borders.thin, colors.outlineVariant) else null
    val text = @Composable {
        Row(Modifier.padding(horizontal = Space.md, vertical = Space.xs + Space.xxs), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
    val sized = modifier.defaultMinSize(minHeight = Sizes.iconLg)
    if (onClick != null) {
        Surface(onClick, sized, shape = shape, color = container, contentColor = content, border = border) { text() }
    } else {
        Surface(sized, shape = shape, color = container, contentColor = content, border = border) { text() }
    }
}

@Composable
fun PatchesCard(title: String, supporting: String, chips: List<String>, more: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.lg), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
                ItemText(title, supporting, Modifier.weight(1f))
                Chevron()
            }
            if (chips.isNotEmpty() || more != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    chips.forEach { Chip(it) }
                    if (more != null) Chip(more, outlined = true)
                }
            }
        }
    }
}

@Composable
fun AboutCard(name: String, version: String, meta: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.xl), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(Space.xxl), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xxl)) {
            Logo(Sizes.aboutLogo)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                ItemText(name, version, titleStyle = MaterialTheme.typography.titleLarge, centered = true)
                Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}
