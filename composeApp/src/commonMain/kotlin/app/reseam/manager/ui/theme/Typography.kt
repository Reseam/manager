package app.reseam.manager.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class ReseamTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val captionMedium: TextStyle,
    val captionSmall: TextStyle,
    val label: TextStyle,
    val chip: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
)

private fun sans(size: Int, weight: FontWeight, lineHeight: Int, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

private fun mono(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = size.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = lineHeight.sp,
)

/** Roles, not sizes: rows use bodyMedium over caption, screen headers use title, step titles use display. */
val ReseamDefaultTypography = ReseamTypography(
    display = sans(32, FontWeight.Bold, 40, letterSpacing = -0.5f),
    headline = sans(26, FontWeight.SemiBold, 32, letterSpacing = -0.4f),
    title = sans(22, FontWeight.SemiBold, 28, letterSpacing = -0.2f),
    titleSmall = sans(18, FontWeight.Medium, 24),
    body = sans(16, FontWeight.Normal, 24),
    bodyMedium = sans(16, FontWeight.Medium, 24),
    bodySmall = sans(15, FontWeight.Normal, 22),
    caption = sans(14, FontWeight.Normal, 20),
    captionMedium = sans(14, FontWeight.Medium, 20),
    captionSmall = sans(13, FontWeight.Normal, 18),
    label = sans(12, FontWeight.Bold, 16, letterSpacing = 1f),
    chip = sans(13, FontWeight.Medium, 16),
    mono = mono(14, 20),
    monoSmall = mono(13, 18),
)
