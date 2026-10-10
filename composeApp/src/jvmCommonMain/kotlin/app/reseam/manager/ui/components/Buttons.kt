package app.reseam.manager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import app.reseam.manager.ui.theme.Borders
import app.reseam.manager.ui.theme.Opacity
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space

enum class ButtonStyle { Filled, Tonal, Outlined, Text }

enum class ButtonSize(internal val height: Dp, internal val icon: Dp) {
    Medium(Sizes.buttonMd, Sizes.iconSm),
    Touch(Sizes.touchTarget, Sizes.iconMd),
    Large(Sizes.buttonLg, Sizes.iconMd),
}

private data class Tones(val container: Color, val content: Color, val border: BorderStroke?)

@Composable
private fun tones(style: ButtonStyle, destructive: Boolean, enabled: Boolean): Tones {
    val colors = MaterialTheme.colorScheme
    val accent = if (destructive) colors.error else colors.primary
    if (!enabled) {
        val content = colors.onSurface.copy(alpha = Opacity.disabledContent)
        return when (style) {
            ButtonStyle.Filled, ButtonStyle.Tonal -> Tones(colors.onSurface.copy(alpha = Opacity.disabledContainer), content, null)
            ButtonStyle.Outlined -> Tones(Color.Transparent, content, BorderStroke(Borders.thin, colors.onSurface.copy(alpha = Opacity.disabledContainer)))
            ButtonStyle.Text -> Tones(Color.Transparent, content, null)
        }
    }
    return when (style) {
        ButtonStyle.Filled -> Tones(if (destructive) colors.error else colors.primary, if (destructive) colors.onError else colors.onPrimary, null)
        ButtonStyle.Tonal -> Tones(colors.secondaryContainer, colors.onSecondaryContainer, null)
        ButtonStyle.Outlined -> Tones(Color.Transparent, accent, BorderStroke(Borders.thin, colors.outlineVariant))
        ButtonStyle.Text -> Tones(Color.Transparent, accent, null)
    }
}

@Composable
fun Button(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Filled,
    size: ButtonSize = if (style == ButtonStyle.Text) ButtonSize.Medium else ButtonSize.Large,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    loading: Boolean = false,
) {
    val tones = tones(style, destructive, enabled || loading)
    val horizontal = if (style == ButtonStyle.Text) Space.md else Space.xxl
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = size.height),
        enabled = enabled && !loading,
        shape = CircleShape,
        color = tones.container,
        contentColor = tones.content,
        border = tones.border,
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(horizontal = horizontal, vertical = Space.sm)),
            horizontalArrangement = Arrangement.spacedBy(Space.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                loading -> Spinner(Modifier.size(size.icon))
                icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(size.icon))
            }
            Text(
                text = label,
                style = if (size == ButtonSize.Medium) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

enum class IconButtonStyle { Standard, Surface, Tonal, Filled, Inverse }

@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Standard,
    size: Dp = Sizes.touchTarget,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (style) {
        IconButtonStyle.Standard -> Color.Transparent to colors.onSurfaceVariant
        IconButtonStyle.Surface -> colors.surfaceContainerHigh to colors.onSurface
        IconButtonStyle.Tonal -> colors.secondaryContainer to colors.onSecondaryContainer
        IconButtonStyle.Filled -> colors.primary to colors.onPrimary
        IconButtonStyle.Inverse -> Color.Transparent to colors.inverseOnSurface
    }
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        shape = CircleShape,
        color = container,
        contentColor = if (enabled) content else colors.onSurface.copy(alpha = Opacity.disabledContent),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(Sizes.iconMd))
        }
    }
}
