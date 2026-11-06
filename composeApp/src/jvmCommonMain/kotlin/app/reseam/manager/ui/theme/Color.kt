package app.reseam.manager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.blend.Blend

@Immutable
data class ExtendedColors(
    val warning: Color,
    val logoOriginal: Color,
    val logoPatch: Color,
    val haloSuccess: Color,
    val haloWarning: Color,
    val haloFailure: Color,
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFA2EBBD),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF075232),
    onPrimaryContainer = Color(0xFFAEF2C6),
    inversePrimary = Color(0xFF296A48),
    secondary = Color(0xFFB8CBBC),
    onSecondary = Color(0xFF233429),
    secondaryContainer = Color(0xFF394B3F),
    onSecondaryContainer = Color(0xFFD4E7D7),
    tertiary = Color(0xFF82D0F7),
    onTertiary = Color(0xFF003547),
    tertiaryContainer = Color(0xFF004D66),
    onTertiaryContainer = Color(0xFFC0E8FF),
    error = Color(0xFFFD736D),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF871F20),
    onErrorContainer = Color(0xFFFFDAD7),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFA1A1A1),
    surfaceTint = Color(0xFFA2EBBD),
    surfaceBright = Color(0xFF393939),
    surfaceDim = Color(0xFF0A0A0A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF171717),
    surfaceContainerHigh = Color(0xFF1F1F1F),
    surfaceContainerHighest = Color(0xFF262626),
    outline = Color(0xFF919191),
    outlineVariant = Color(0xFF393939),
    inverseSurface = Color(0xFFE2E2E2),
    inverseOnSurface = Color(0xFF303030),
    scrim = Color(0xFF000000),
)

internal val LightColors = lightColorScheme(
    primary = Color(0xFF296A48),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA2EBBD),
    onPrimaryContainer = Color(0xFF003920),
    inversePrimary = Color(0xFFA2EBBD),
    secondary = Color(0xFF516356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4E7D7),
    onSecondaryContainer = Color(0xFF394B3F),
    tertiary = Color(0xFF006686),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC0E8FF),
    onTertiaryContainer = Color(0xFF004D66),
    error = Color(0xFFA73735),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD7),
    onErrorContainer = Color(0xFF871F20),
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFF9F9F9),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = Color(0xFF474747),
    surfaceTint = Color(0xFF296A48),
    surfaceBright = Color(0xFFF9F9F9),
    surfaceDim = Color(0xFFE2E2E2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE2E2E2),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFC6C6C6),
    inverseSurface = Color(0xFF303030),
    inverseOnSurface = Color(0xFFF1F1F1),
    scrim = Color(0xFF000000),
)

internal val DarkExtendedColors = ExtendedColors(
    warning = Color(0xFFF6AF00),
    logoOriginal = Color(0xFF51AE79),
    logoPatch = Color(0xFFA2EBBD),
    haloSuccess = Color(0xFFA2EBBD),
    haloWarning = Color(0xFFF6AF00),
    haloFailure = Color(0xFFFD736D),
)

internal val LightExtendedColors = ExtendedColors(
    warning = Color(0xFF7D5700),
    logoOriginal = Color(0xFF296A48),
    logoPatch = Color(0xFF77B991),
    haloSuccess = Color(0xFFA2EBBD),
    haloWarning = Color(0xFFFFD68A),
    haloFailure = Color(0xFFFFB3AE),
)

/**
 * Reseam's own colors under a wallpaper [scheme]. Halos take the roles their rings come from, as Reseam's success halos
 * do, and the warning amber shifts toward the wallpaper's primary, as Material harmonizes custom colors. The logo keeps
 * Reseam's colors.
 */
internal fun wallpaperExtendedColors(scheme: ColorScheme, dark: Boolean): ExtendedColors {
    val reseam = if (dark) DarkExtendedColors else LightExtendedColors
    fun harmonized(color: Color) = Color(Blend.harmonize(color.toArgb(), scheme.primary.toArgb()))
    return reseam.copy(
        warning = harmonized(reseam.warning),
        haloSuccess = if (dark) scheme.primary else scheme.primaryContainer,
        haloWarning = harmonized(reseam.haloWarning),
        haloFailure = if (dark) scheme.error else scheme.errorContainer,
    )
}

internal val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
