package app.reseam.manager.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.resources.*
import app.reseam.manager.ui.theme.Borders
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Opacity
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

class ItemGroupScope internal constructor() {
    internal val items = mutableListOf<GroupItem>()

    fun item(key: Any? = null, content: @Composable (shape: Shape) -> Unit) {
        items += GroupItem(key, content)
    }
}

internal class GroupItem(val key: Any?, val content: @Composable (Shape) -> Unit)

private fun groupShape(index: Int, count: Int): Shape {
    val top = if (index == 0) Radius.lg else Radius.sm
    val bottom = if (index == count - 1) Radius.lg else Radius.sm
    return RoundedCornerShape(top, top, bottom, bottom)
}

@Composable
fun ItemGroup(modifier: Modifier = Modifier, content: ItemGroupScope.() -> Unit) {
    val items = ItemGroupScope().apply(content).items
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        items.forEachIndexed { index, item -> item.content(groupShape(index, items.size)) }
    }
}

fun LazyListScope.itemGroup(top: Dp = 0.dp, content: ItemGroupScope.() -> Unit) {
    val items = ItemGroupScope().apply(content).items
    items.forEachIndexed { index, item ->
        item(key = item.key) {
            if (index == 0 && top > 0.dp) Box(Modifier.padding(top = top)) { item.content(groupShape(index, items.size)) } else item.content(groupShape(index, items.size))
        }
    }
}

@Composable
fun GroupedItem(
    title: String,
    shape: Shape,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selected: Boolean = false,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    GroupedItemFrame(shape, selected, modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), icon, iconTint) {
        ItemText(title, supporting, Modifier.weight(1f), titleStyle = MaterialTheme.typography.bodyLarge, titleColor = titleColor)
        trailing?.invoke()
    }
}

@Composable
fun SwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    GroupedItemFrame(
        shape = shape,
        selected = false,
        modifier = modifier.toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange).alpha(if (enabled) 1f else Opacity.disabledContent),
        icon = icon,
    ) {
        ItemText(title, supporting, Modifier.weight(1f), titleStyle = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun GroupedItemFrame(shape: Shape, selected: Boolean, modifier: Modifier, icon: ImageVector?, iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant, content: @Composable RowScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainer)
            .then(modifier)
            .defaultMinSize(minHeight = Sizes.appIconSm + Space.md * 2)
            .padding(horizontal = Space.lg, vertical = Space.md),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(Sizes.iconMd))
        content()
    }
}

@Composable
fun Chevron(expanded: Boolean? = null) {
    Icon(
        imageVector = if (expanded == null) Icons.ChevronRight else Icons.ChevronDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(Sizes.iconMd).rotate(if (expanded == true) 180f else 0f),
    )
}

@Composable
fun SelectedMark() {
    Icon(Icons.Check, contentDescription = stringResource(Res.string.selected), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Sizes.iconMd))
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(vertical = Space.sm), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun FeaturedHeader(title: String, supporting: String?, modifier: Modifier = Modifier) {
    ItemText(title, supporting, modifier, titleStyle = MaterialTheme.typography.labelLarge, titleColor = MaterialTheme.colorScheme.primary)
}

@Composable
fun AppRow(app: AppLook, supporting: String, onClick: () -> Unit, modifier: Modifier = Modifier, margin: Dp = Layout.margin, selected: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = margin),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, Sizes.appIconLg)
        ItemText(app.name, supporting, Modifier.weight(1f))
    }
}

@Composable
private fun CheckboxMark(checked: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.xs)
    val tone = if (enabled) 1f else Opacity.disabledContent
    Box(
        modifier = modifier
            .size(Sizes.iconSm)
            .alpha(tone)
            .then(if (checked) Modifier.background(colors.primary, shape) else Modifier.border(Borders.thick, colors.onSurfaceVariant, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun PatchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    note: String? = null,
    expanded: Boolean? = null,
    onExpand: (() -> Unit)? = null,
    options: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
                .padding(start = Layout.margin, end = if (onExpand != null) Space.xs else Layout.margin, top = Space.md, bottom = Space.md),
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            CheckboxMark(checked, enabled, Modifier.padding(top = Space.xxs))
            Column(Modifier.weight(1f).alpha(if (enabled) 1f else Opacity.disabledContent), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                if (description.isNotEmpty()) Text(description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                if (note != null) Text(note, style = MaterialTheme.typography.bodyMedium, color = colors.error)
            }
            if (onExpand != null) {
                Box(Modifier.size(Sizes.touchTarget).clip(CircleShape).clickable(onClick = onExpand), contentAlignment = Alignment.Center) {
                    Chevron(expanded == true)
                }
            }
        }
        if (expanded == true) {
            Column(
                Modifier.fillMaxWidth().padding(start = Layout.margin + Sizes.iconSm + Space.lg, end = Layout.margin, bottom = Space.md),
                verticalArrangement = Arrangement.spacedBy(Space.md),
                content = options,
            )
        }
    }
}

enum class ProgressState { Pending, Running, Applied, Failed, Skipped }

@Composable
fun ProgressRow(title: String, state: ProgressState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Layout.margin, vertical = Space.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (icon, tint) = when (state) {
            ProgressState.Pending -> Icons.CircleDashed to colors.onSurfaceVariant
            ProgressState.Skipped -> Icons.CircleMinus to colors.onSurfaceVariant
            ProgressState.Running -> Icons.Loader to colors.primary
            ProgressState.Applied -> Icons.CircleCheck to colors.primary
            ProgressState.Failed -> Icons.CircleX to colors.error
        }
        val spin = if (state == ProgressState.Running) {
            rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart)).value
        } else {
            0f
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Sizes.iconMd).rotate(spin))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (state == ProgressState.Pending || state == ProgressState.Skipped) colors.onSurfaceVariant else colors.onSurface,
        )
    }
}
