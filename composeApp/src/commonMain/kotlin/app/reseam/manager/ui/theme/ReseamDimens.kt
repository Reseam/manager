package app.reseam.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ReseamDimens(
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space5: Dp = 20.dp,
    val space6: Dp = 24.dp,
    val space8: Dp = 32.dp,
    val space10: Dp = 40.dp,
    val space12: Dp = 48.dp,
    val space16: Dp = 64.dp,
    val phoneWidth: Dp = 420.dp,
    val topBarHeight: Dp = 64.dp,
    val iconSmall: Dp = 20.dp,
    val iconStandard: Dp = 24.dp,
    val iconContainer: Dp = 40.dp,
)

@Immutable
data class ReseamShapes(
    val small: Shape = RoundedCornerShape(8.dp),
    val medium: Shape = RoundedCornerShape(12.dp),
    val card: Shape = RoundedCornerShape(14.dp),
    val large: Shape = RoundedCornerShape(16.dp),
    val sheet: Shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    val pill: Shape = RoundedCornerShape(50),
)

val ReseamDefaultDimens = ReseamDimens()
val ReseamDefaultShapes = ReseamShapes()
