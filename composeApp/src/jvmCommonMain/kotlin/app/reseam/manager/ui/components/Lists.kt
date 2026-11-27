package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.resources.*
import app.reseam.manager.ui.nav.ContainerEnd
import app.reseam.manager.ui.nav.SharedKeys
import app.reseam.manager.ui.nav.sharedIcon
import app.reseam.manager.ui.nav.sharedText
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
    supportingStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    supportingLines: Int = Int.MAX_VALUE,
    titleColor: Color = Color.Unspecified,
    icon: ImageVector? = null,
    iconTint: Color = titleColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant },
    leading: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val start = leading ?: icon?.let { { ItemIcon(it, iconTint) } }
    GroupedItemFrame(shape, selected, modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), start) {
        ItemText(title, supporting, Modifier.weight(1f), titleStyle = MaterialTheme.typography.bodyLarge, titleColor = titleColor.takeOrElse { MaterialTheme.colorScheme.onSurface }, supportingStyle = supportingStyle, supportingLines = supportingLines)
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
) = ToggleItem(title, checked, onCheckedChange, shape, modifier, supporting, icon, enabled, Role.Switch) {
    Switch(checked = checked, onCheckedChange = null, enabled = enabled)
}

@Composable
fun CheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) = ToggleItem(title, checked, onCheckedChange, shape, modifier, supporting, icon, enabled, Role.Checkbox) {
    Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
}

@Composable
private fun ToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: Shape,
    modifier: Modifier,
    supporting: String?,
    icon: ImageVector?,
    enabled: Boolean,
    role: Role,
    control: @Composable () -> Unit,
) {
    GroupedItemFrame(
        shape = shape,
        selected = false,
        modifier = modifier.toggleable(checked, enabled = enabled, role = role, onValueChange = onCheckedChange).alpha(if (enabled) 1f else Opacity.disabledContent),
        leading = icon?.let { { ItemIcon(it) } },
    ) {
        ItemText(title, supporting, Modifier.weight(1f), titleStyle = MaterialTheme.typography.bodyLarge)
        control()
    }
}

@Composable
private fun ItemIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Sizes.iconMd))
}

@Composable
private fun GroupedItemFrame(shape: Shape, selected: Boolean, modifier: Modifier, leading: (@Composable () -> Unit)?, content: @Composable RowScope.() -> Unit) {
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
        leading?.invoke()
        content()
    }
}

@Composable
fun Chevron(expanded: Boolean? = null) {
    TrailingIcon(if (expanded == null) Icons.ChevronRight else Icons.ChevronDown, Modifier.rotate(if (expanded == true) 180f else 0f))
}

@Composable
fun ExternalMark() {
    TrailingIcon(Icons.ExternalLink)
}

@Composable
private fun TrailingIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier.size(Sizes.iconMd))
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
        AppIcon(app, Sizes.appIconLg, Modifier.sharedIcon(SharedKeys.appIcon(app.packageName), ContainerEnd.Opener))
        ItemText(
            app.name,
            supporting,
            Modifier.weight(1f),
            titleModifier = Modifier.sharedText(SharedKeys.appName(app.packageName), ContainerEnd.Opener),
            supportModifier = Modifier.sharedText(SharedKeys.appSupport(app.packageName), ContainerEnd.Opener),
        )
    }
}
