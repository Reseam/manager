package app.reseam.manager.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.theme.extendedTypography
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownPadding

@Composable
fun MarkdownText(state: State, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val body = type.bodyMedium.copy(color = colors.onSurfaceVariant)
    val heading = type.titleSmall.copy(color = colors.onSurface)
    Markdown(
        state = state,
        modifier = modifier,
        colors = markdownColor(text = colors.onSurfaceVariant, codeBackground = colors.surfaceContainerHighest),
        typography = markdownTypography(
            h1 = type.titleLarge.copy(color = colors.onSurface),
            h2 = type.titleMedium.copy(color = colors.onSurface),
            h3 = heading,
            h4 = heading,
            h5 = heading,
            h6 = heading,
            text = body,
            code = MaterialTheme.extendedTypography.monoSmall,
            quote = body,
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
            textLink = TextLinkStyles(SpanStyle(color = colors.primary, fontWeight = FontWeight.Medium)),
        ),
        padding = markdownPadding(block = Space.xs, listIndent = Space.sm, codeBlock = PaddingValues(Space.md)),
    )
}
