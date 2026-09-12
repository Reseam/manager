package app.reseam.manager.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ReseamColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSunken: Color,
    val muted: Color,
    val mutedElevated: Color,
    val foreground: Color,
    val mutedForeground: Color,
    val subtleForeground: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val primary: Color,
    val primaryBright: Color,
    val primaryDarker: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val primaryFaint: Color,
    val primaryHairline: Color,
    val warning: Color,
    val warningForeground: Color,
    val warningSoft: Color,
    val warningHairline: Color,
    val dangerForeground: Color,
    val dangerHairline: Color,
    val scrim: Color,
)

private val Mint = Color(0xFFB6F0CF)

val ReseamDarkColors = ReseamColors(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF1A1A1A),
    surfaceElevated = Color(0xFF1F1F1F),
    surfaceSunken = Color(0xFF101010),
    muted = Color(0xFF1C1C1C),
    mutedElevated = Color(0xFF242424),
    foreground = Color(0xFFEDEDED),
    mutedForeground = Color(0xFFA3A3A3),
    subtleForeground = Color(0xFF666666),
    border = Color(0xFF333333),
    borderStrong = Color(0xFF404040),
    divider = Color(0xFF2A2A2A),
    primary = Mint,
    primaryBright = Color(0xFFC9F4DB),
    primaryDarker = Color(0xFF6BC58E),
    onPrimary = Color(0xFF000000),
    primarySoft = Mint.copy(alpha = 0.20f),
    primaryFaint = Mint.copy(alpha = 0.08f),
    primaryHairline = Mint.copy(alpha = 0.30f),
    warning = Color(0xFFE9B860),
    warningForeground = Color(0xFFE6C078),
    warningSoft = Color(0xFFE9B860).copy(alpha = 0.12f),
    warningHairline = Color(0xFFE9B860).copy(alpha = 0.25f),
    dangerForeground = Color(0xFFFFB4B4),
    dangerHairline = Color(0xFF3A1A1A),
    scrim = Color(0x99000000),
)
