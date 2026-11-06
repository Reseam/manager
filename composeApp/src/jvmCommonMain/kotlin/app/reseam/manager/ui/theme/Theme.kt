package app.reseam.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable

private val ReseamShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl),
)

/** What the app draws with: Material's color roles and Reseam's own. */
@Immutable
class ReseamColors(val scheme: ColorScheme, val extended: ExtendedColors)

/** Reseam's colors, or the wallpaper's when [dynamicColor] asks for them and Android has them. */
@Composable
fun reseamColors(dark: Boolean, dynamicColor: Boolean): ReseamColors {
    val wallpaper = if (dynamicColor) dynamicColorScheme(dark) else null
    return when {
        wallpaper != null -> ReseamColors(wallpaper, wallpaperExtendedColors(wallpaper, dark))
        dark -> ReseamColors(DarkColors, DarkExtendedColors)
        else -> ReseamColors(LightColors, LightExtendedColors)
    }
}

@Composable
fun ReseamTheme(colors: ReseamColors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides colors.extended, LocalExtendedTypography provides reseamExtendedTypography()) {
        MaterialTheme(colorScheme = colors.scheme, typography = reseamTypography(), shapes = ReseamShapes) {
            CompositionLocalProvider(LocalContentColor provides colors.scheme.onSurface, content = content)
        }
    }
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current

val MaterialTheme.extendedTypography: ExtendedTypography
    @Composable @ReadOnlyComposable get() = LocalExtendedTypography.current
