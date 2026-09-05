package app.reseam.manager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

@Immutable
data class ReseamShapes(
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val card: RoundedCornerShape = RoundedCornerShape(14.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val hero: RoundedCornerShape = RoundedCornerShape(22.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50),
)

val ReseamDefaultShapes = ReseamShapes()
