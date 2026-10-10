package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.reseam.manager.resources.*
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopBar(
    title: String?,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().height(Sizes.topBar).padding(start = if (onBack != null) Space.md else Layout.margin, end = Layout.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconButton(Icons.ArrowLeft, stringResource(Res.string.back), onBack, style = IconButtonStyle.Standard)
        Text(
            text = title.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        actions()
    }
}

@Composable
fun BrandTopBar(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(Sizes.topBar).padding(horizontal = Layout.margin),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo(Sizes.iconLg)
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.height(Sizes.searchBar).background(colors.surfaceContainerHigh, CircleShape).padding(horizontal = Space.lg),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(Sizes.iconMd))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    field()
                }
            },
        )
        if (query.isNotEmpty()) IconButton(Icons.Close, stringResource(Res.string.clear_search), onClick = { onQueryChange("") }, size = Sizes.iconMd)
    }
}

private enum class BottomBarSlot { Bar, Content }

@Composable
fun BottomBarLayout(
    bar: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    margin: Dp = Layout.margin,
    content: @Composable (PaddingValues) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        val bars = subcompose(BottomBarSlot.Bar) { BottomBar(margin, bar) }.map { it.measure(constraints.copy(minHeight = 0)) }
        val barHeight = bars.maxOfOrNull { it.height } ?: 0
        val contents = subcompose(BottomBarSlot.Content) { content(PaddingValues(bottom = barHeight.toDp())) }.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            contents.forEach { it.place(0, 0) }
            bars.forEach { it.place(0, constraints.maxHeight - it.height) }
        }
    }
}

@Composable
private fun BottomBar(margin: Dp, content: @Composable RowScope.() -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val fade = Space.xl5.toPx() / size.height
                drawRect(Brush.verticalGradient(0f to Color.Transparent, fade to surface, 1f to surface))
            }
            .padding(start = margin, end = margin, top = Space.xl5, bottom = Space.lg)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
        verticalAlignment = Alignment.Bottom,
        content = content,
    )
}
