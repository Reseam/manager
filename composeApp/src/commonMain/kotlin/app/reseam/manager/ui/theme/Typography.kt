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

val ReseamDefaultTypography = ReseamTypography(
    display = sans(28, FontWeight.Bold, 34, letterSpacing = -0.5f),
    headline = sans(24, FontWeight.SemiBold, 30, letterSpacing = -0.4f),
    title = sans(20, FontWeight.SemiBold, 26, letterSpacing = -0.2f),
    titleSmall = sans(16, FontWeight.Medium, 22),
    body = sans(16, FontWeight.Normal, 24),
    bodyMedium = sans(16, FontWeight.Medium, 22),
    bodySmall = sans(14, FontWeight.Normal, 20),
    caption = sans(13, FontWeight.Normal, 18),
    captionMedium = sans(13, FontWeight.Medium, 18),
    captionSmall = sans(12, FontWeight.Normal, 16),
    label = sans(11, FontWeight.Bold, 14, letterSpacing = 1.2f),
    mono = mono(13, 18),
    monoSmall = mono(11, 15),
)
