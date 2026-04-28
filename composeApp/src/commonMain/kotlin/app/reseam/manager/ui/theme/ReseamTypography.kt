package app.reseam.manager.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

@Immutable
data class ReseamTypography(
    val sans: FontFamily,
    val mono: FontFamily,
    val displayLarge: TextStyle,
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val caption: TextStyle,
    val captionSmall: TextStyle,
    val label: TextStyle,
    val labelLarge: TextStyle,
    val mono12: TextStyle,
    val mono10: TextStyle,
    val numeric: TextStyle,
)

// Inter ships only as WOFF2 in manager-design/, which Compose/Skia cannot decode.
// Drop InterVariable.ttf into composeResources/font/ and swap FontFamily.SansSerif
// for the loaded family to enable.
private val sansFamily = FontFamily.SansSerif
private val monoFamily = FontFamily.Monospace

private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val ReseamDarkTypography = ReseamTypography(
    sans = sansFamily,
    mono = monoFamily,
    displayLarge = TextStyle(
        fontFamily = sansFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp,
        lineHeightStyle = tightLineHeight,
    ),
    display = TextStyle(
        fontFamily = sansFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
        lineHeightStyle = tightLineHeight,
    ),
    headline = TextStyle(
        fontFamily = sansFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp,
        lineHeightStyle = tightLineHeight,
    ),
    title = TextStyle(
        fontFamily = sansFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = sansFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
    ),
    body = TextStyle(
        fontFamily = sansFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = sansFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
    ),
    caption = TextStyle(
        fontFamily = sansFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
    ),
    captionSmall = TextStyle(
        fontFamily = sansFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 17.sp,
    ),
    label = TextStyle(
        fontFamily = sansFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 15.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = sansFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 16.sp,
    ),
    mono12 = TextStyle(
        fontFamily = monoFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
    ),
    mono10 = TextStyle(
        fontFamily = monoFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 15.sp,
    ),
    numeric = TextStyle(
        fontFamily = sansFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 26.sp,
    ),
)
