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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = ReseamTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

private val SlotWidth = 40.dp

@Composable
fun BottomBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/** Top bar, scrolling content, optional bottom bar. Every route renders inside one of these. */
@Composable
fun Screen(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    content: LazyListScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().background(ReseamTheme.colors.background)) {
        TopBar(title = title, onBack = onBack, actions = actions)
        header?.invoke()
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = contentPadding,
            content = content,
        )
        bottomBar?.invoke()
    }
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
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = ReseamTheme.typography.titleSmall, color = colors.foreground, textAlign = TextAlign.Center)
        Text(body, style = ReseamTheme.typography.caption, color = colors.mutedForeground, textAlign = TextAlign.Center)
    }
}
