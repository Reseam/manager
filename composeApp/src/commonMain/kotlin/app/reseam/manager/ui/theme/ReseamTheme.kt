package app.reseam.manager.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle

private val LocalReseamColors: ProvidableCompositionLocal<ReseamColors> =
    staticCompositionLocalOf { ReseamDarkColors }

private val LocalReseamTypography: ProvidableCompositionLocal<ReseamTypography> =
    staticCompositionLocalOf { ReseamDarkTypography }

private val LocalReseamDimens: ProvidableCompositionLocal<ReseamDimens> =
    staticCompositionLocalOf { ReseamDefaultDimens }

private val LocalReseamShapes: ProvidableCompositionLocal<ReseamShapes> =
    staticCompositionLocalOf { ReseamDefaultShapes }

private val LocalReseamMotion: ProvidableCompositionLocal<ReseamMotion> =
    staticCompositionLocalOf { ReseamDefaultMotion }

object ReseamTheme {
    val colors: ReseamColors
        @Composable @ReadOnlyComposable
        get() = LocalReseamColors.current

    val typography: ReseamTypography
        @Composable @ReadOnlyComposable
        get() = LocalReseamTypography.current

    val dimens: ReseamDimens
        @Composable @ReadOnlyComposable
        get() = LocalReseamDimens.current

    val shapes: ReseamShapes
        @Composable @ReadOnlyComposable
        get() = LocalReseamShapes.current

    val motion: ReseamMotion
        @Composable @ReadOnlyComposable
        get() = LocalReseamMotion.current
}

@Composable
fun ReseamTheme(content: @Composable () -> Unit) {
    val colors = ReseamDarkColors
    val typography = ReseamDarkTypography
    val materialColorScheme = darkColorScheme(
        primary = colors.primary,
        onPrimary = colors.primaryForeground,
        secondary = colors.primaryDarker,
        background = colors.background,
        onBackground = colors.foreground,
        surface = colors.card,
        onSurface = colors.foreground,
        surfaceVariant = colors.muted,
        onSurfaceVariant = colors.mutedForeground,
        outline = colors.border,
        error = colors.warning,
        onError = colors.background,
    )
    CompositionLocalProvider(
        LocalReseamColors provides colors,
        LocalReseamTypography provides typography,
        LocalReseamDimens provides ReseamDefaultDimens,
        LocalReseamShapes provides ReseamDefaultShapes,
        LocalReseamMotion provides ReseamDefaultMotion,
        LocalContentColor provides colors.foreground,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content,
        )
    }
}

internal val MonoTextStyle: TextStyle
    @Composable get() = ReseamTheme.typography.mono12
