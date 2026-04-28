package app.reseam.manager.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ReseamColors(
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val surfaceSunken: Color,
    val surfaceInset: Color,
    val muted: Color,
    val mutedElevated: Color,
    val accent: Color,
    val foreground: Color,
    val mutedForeground: Color,
    val subtleForeground: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val input: Color,
    val primary: Color,
    val primaryBright: Color,
    val primaryForeground: Color,
    val primaryDarker: Color,
    val primarySoft: Color,
    val primaryFaint: Color,
    val primaryHairline: Color,
    val primaryGlow: Color,
    val ring: Color,
    val warning: Color,
    val warningForeground: Color,
    val warningSoft: Color,
    val warningHairline: Color,
    val destructive: Color,
    val destructiveForeground: Color,
    val destructiveHairline: Color,
    val info: Color,
    val logError: Color,
    val scrim: Color,
)

private val Mint = Color(0xFFB6F0CF)
private val MintDarker = Color(0xFF6BC58E)
private val Amber = Color(0xFFE9B860)
private val Blue = Color(0xFF4F86FF)

val ReseamDarkColors = ReseamColors(
    background = Color(0xFF0A0A0A),
    card = Color(0xFF121212),
    cardElevated = Color(0xFF131313),
    surfaceSunken = Color(0xFF0E0E0E),
    surfaceInset = Color(0xFF0F0F0F),
    muted = Color(0xFF171717),
    mutedElevated = Color(0xFF1A1A1A),
    accent = Color(0xFF1F1F1F),
    foreground = Color(0xFFEDEDED),
    mutedForeground = Color(0xFFA3A3A3),
    subtleForeground = Color(0xFF666666),
    border = Color(0xFF262626),
    borderStrong = Color(0xFF2A2A2A),
    divider = Color(0xFF1A1A1A),
    input = Color(0xFF262626),
    primary = Mint,
    primaryBright = Color(0xFFC9F4DB),
    primaryForeground = Color(0xFF000000),
    primaryDarker = MintDarker,
    primarySoft = Color(0x33B6F0CF),
    primaryFaint = Color(0x14B6F0CF),
    primaryHairline = Color(0x4DB6F0CF),
    primaryGlow = Color(0x66B6F0CF),
    ring = Mint,
    warning = Amber,
    warningForeground = Color(0xFFE6C078),
    warningSoft = Color(0x1FE9B860),
    warningHairline = Color(0x40E9B860),
    destructive = Color(0xFF7F1D1D),
    destructiveForeground = Color(0xFFFFB4B4),
    destructiveHairline = Color(0xFF3A1A1A),
    info = Blue,
    logError = Color(0xFFFF8A8A),
    scrim = Color(0x99000000),
)
