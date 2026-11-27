package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.reseam.manager.resources.*
import app.reseam.manager.ui.theme.Borders
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Opacity
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.theme.extendedColors
import org.jetbrains.compose.resources.stringResource

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

enum class NoteTone { Info, Caution, Error }

data class RowNote(val text: String, val tone: NoteTone)

@Composable
fun NoteText(note: RowNote, modifier: Modifier = Modifier) {
    val color = when (note.tone) {
        NoteTone.Info -> MaterialTheme.colorScheme.primary
        NoteTone.Caution -> MaterialTheme.extendedColors.warning
        NoteTone.Error -> MaterialTheme.colorScheme.error
    }
    Text(note.text, modifier, style = MaterialTheme.typography.bodyMedium, color = color)
}

@Composable
fun PatchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    notes: List<RowNote> = emptyList(),
    expanded: Boolean? = null,
    onExpand: (() -> Unit)? = null,
    options: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().then(if (expanded == true) Modifier.optionGuide(colors.outlineVariant) else Modifier)) {
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
                notes.forEach { NoteText(it) }
            }
            if (onExpand != null) {
                IconButton(
                    icon = if (expanded == true) Icons.ChevronUp else Icons.ChevronDown,
                    contentDescription = stringResource(if (expanded == true) Res.string.patches_hide_options else Res.string.patches_show_options),
                    onClick = onExpand,
                )
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

private fun Modifier.optionGuide(color: Color) = drawBehind {
    val inset = (Layout.margin + Sizes.iconSm / 2).toPx()
    val x = if (layoutDirection == LayoutDirection.Ltr) inset else size.width - inset
    val top = (Space.md + Space.xxs + Sizes.iconSm + Space.sm).toPx()
    drawLine(color, Offset(x, top), Offset(x, size.height - Space.xs.toPx()), Borders.thick.toPx(), StrokeCap.Round)
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
        val size = Modifier.size(Sizes.iconMd)
        Icon(icon, contentDescription = null, tint = tint, modifier = if (state == ProgressState.Running) size.spinning(1000) else size)
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (state == ProgressState.Pending || state == ProgressState.Skipped) colors.onSurfaceVariant else colors.onSurface,
        )
    }
}
