package app.reseam.manager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import app.reseam.manager.resources.*
import org.jetbrains.compose.resources.Font

private val EvenLeading = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

@Composable
internal fun reseamTypography(): Typography {
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
    )
    return remember(inter) {
        fun style(size: Int, lineHeight: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
            fontFamily = inter,
            fontSize = size.sp,
            lineHeight = lineHeight.sp,
            fontWeight = weight,
            letterSpacing = tracking.sp,
            lineHeightStyle = EvenLeading,
        )
        Typography(
            displayLarge = style(57, 64, FontWeight.SemiBold, -1.42),
            displayMedium = style(45, 52, FontWeight.SemiBold, -1.12),
            displaySmall = style(36, 44, FontWeight.SemiBold, -0.9),
            headlineLarge = style(32, 40, FontWeight.SemiBold, -0.8),
            headlineMedium = style(28, 36, FontWeight.SemiBold, -0.7),
            headlineSmall = style(24, 32, FontWeight.SemiBold, -0.48),
            titleLarge = style(22, 28, FontWeight.SemiBold, -0.22),
            titleMedium = style(16, 24, FontWeight.SemiBold),
            titleSmall = style(14, 20, FontWeight.Medium),
            bodyLarge = style(16, 24, FontWeight.Normal),
            bodyMedium = style(14, 20, FontWeight.Normal),
            bodySmall = style(12, 16, FontWeight.Normal),
            labelLarge = style(14, 20, FontWeight.Medium),
            labelMedium = style(12, 16, FontWeight.Medium),
            labelSmall = style(11, 16, FontWeight.Medium),
        )
    }
}
