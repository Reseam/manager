package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

private val SlotWidth = 44.dp

@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = ReseamTheme.colors
    val layout = ReseamTheme.layout
    Row(
        modifier = modifier.fillMaxWidth().height(if (layout.compact) 56.dp else 64.dp).padding(horizontal = layout.pageMargin - 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.widthIn(min = SlotWidth), contentAlignment = Alignment.CenterStart) {
            if (onBack != null) IconButton(Icons.ArrowLeft, "Back", onBack, size = SlotWidth, tint = colors.foreground)
        }
        Text(
            text = title,
            style = ReseamTheme.typography.titleSmall,
            color = colors.foreground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.widthIn(min = SlotWidth),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/**
 * Actions pinned under the content. [fill] makes a button span the bar on compact widths and
 * gives it a comfortable minimum on wider ones, where the actions sit at the end.
 */
@Composable
fun BottomBar(modifier: Modifier = Modifier, content: @Composable RowScope.(fill: Modifier) -> Unit) {
    val colors = ReseamTheme.colors
    val layout = ReseamTheme.layout
    Column(modifier = modifier.fillMaxWidth()) {
        Divider()
        Box(
            modifier = Modifier.fillMaxWidth().background(colors.background).padding(horizontal = layout.pageMargin, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                modifier = Modifier.widthIn(max = layout.contentMaxWidth).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, if (layout.compact) Alignment.Start else Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content(if (layout.compact) Modifier.weight(1f) else Modifier.widthIn(min = 200.dp))
            }
        }
    }
}

/**
 * Top bar, body, optional bottom bar. Every route renders inside one of these.
 * The body is centered and capped at the layout's reading width, or the wide width for two-column bodies.
 * A screen shown as the detail beside its list has no back button; the list is already there.
 */
@Composable
fun ScreenFrame(
    title: String?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    wide: Boolean = false,
    content: @Composable () -> Unit,
) {
    val layout = ReseamTheme.layout
    val inDetailPane = LocalPaneRole.current == PaneRole.Detail
    Column(modifier = modifier.fillMaxSize().background(ReseamTheme.colors.background)) {
        if (title != null) TopBar(title = title, onBack = onBack.takeUnless { inDetailPane }, actions = actions)
        header?.invoke()
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = if (wide) layout.wideContentMaxWidth else layout.contentMaxWidth).fillMaxSize()) { content() }
        }
        bottomBar?.invoke()
    }
}

/** A [ScreenFrame] whose body is one scrolling column, inset by the page margin, items 8dp apart. */
@Composable
fun Screen(
    title: String?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    ScreenFrame(title, modifier, onBack, actions, header, bottomBar) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = ReseamTheme.layout.pageMargin, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** Uppercase label above a group, spaced to separate it from the group before. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    SectionLabel(text, modifier = modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp), trailing = trailing)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val colors = ReseamTheme.colors
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = ReseamTheme.typography.label, color = colors.mutedForeground, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = ReseamTheme.typography.captionSmall, color = colors.mutedForeground)
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = ReseamTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) IconTile(icon, size = 56.dp, background = colors.muted, tint = colors.mutedForeground, modifier = Modifier.padding(bottom = 8.dp))
        Text(title, style = ReseamTheme.typography.titleSmall, color = colors.foreground, textAlign = TextAlign.Center)
        Text(body, style = ReseamTheme.typography.bodySmall, color = colors.mutedForeground, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
        if (actions != null) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                content = actions,
            )
        }
    }
}
