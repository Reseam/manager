package app.reseam.manager.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColors = staticCompositionLocalOf { ReseamDarkColors }
private val LocalTypography = staticCompositionLocalOf { ReseamDefaultTypography }
private val LocalMotion = staticCompositionLocalOf { ReseamDefaultMotion }
private val LocalShapes = staticCompositionLocalOf { ReseamDefaultShapes }

object ReseamTheme {
    val colors: ReseamColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val typography: ReseamTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current
    val motion: ReseamMotion
        @Composable @ReadOnlyComposable get() = LocalMotion.current
    val shapes: ReseamShapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current
}

@Composable
fun ReseamTheme(content: @Composable () -> Unit) {
    val colors = ReseamDarkColors
    val scheme = darkColorScheme(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        secondary = colors.primaryDarker,
        background = colors.background,
        onBackground = colors.foreground,
        surface = colors.surface,
        onSurface = colors.foreground,
        surfaceVariant = colors.muted,
        onSurfaceVariant = colors.mutedForeground,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceElevated,
        outline = colors.border,
        outlineVariant = colors.divider,
        error = colors.warning,
        onError = colors.background,
        scrim = colors.scrim,
    )
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides ReseamDefaultTypography,
        LocalMotion provides ReseamDefaultMotion,
        LocalShapes provides ReseamDefaultShapes,
        LocalContentColor provides colors.foreground,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
