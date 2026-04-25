package app.reseam.manager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RsLogoMark(
    size: Dp = 22.dp,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val image = remember(colors.primary, colors.primaryDarker) {
        ImageVector.Builder(
            name = "ReseamLogo",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).addPath(
            pathData = addPathNodes(
                "M6 8.5C6 6.8 7.3 5.5 9 5.5H16.5C19 5.5 20.5 7.5 20.5 10C20.5 13 18.5 14 16.5 14H12C10.3 14 9 15.3 9 17V23.5C9 25.2 7.7 26.5 6 26.5V8.5Z",
            ),
            fill = SolidColor(colors.primaryDarker),
        ).addPath(
            pathData = addPathNodes(
                "M13 16.5C13 14.8 14.3 13.5 16 13.5H23C25 13.5 26.5 15 26.5 17V20C26.5 22 25 23.5 23 23.5H16C14.3 23.5 13 22.2 13 20.5V16.5Z",
            ),
            fill = SolidColor(colors.primary),
        ).build()
    }
    Image(
        imageVector = image,
        contentDescription = "Reseam",
        modifier = modifier.size(size),
    )
}
