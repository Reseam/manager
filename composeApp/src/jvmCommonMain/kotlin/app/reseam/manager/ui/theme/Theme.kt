package app.reseam.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val ReseamShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)

@Composable
fun ReseamTheme(dark: Boolean, dynamicColor: Boolean, content: @Composable () -> Unit) {
    val reseamColors = if (dark) DarkColors else LightColors
    val colors = if (dynamicColor) dynamicColorScheme(dark) ?: reseamColors else reseamColors
    CompositionLocalProvider(LocalExtendedColors provides if (dark) DarkExtendedColors else LightExtendedColors, LocalExtendedTypography provides reseamExtendedTypography()) {
        MaterialTheme(colorScheme = colors, typography = reseamTypography(), shapes = ReseamShapes) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface, content = content)
        }
    }
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

val MaterialTheme.extendedTypography: ExtendedTypography
    @Composable @ReadOnlyComposable get() = LocalExtendedTypography.current
