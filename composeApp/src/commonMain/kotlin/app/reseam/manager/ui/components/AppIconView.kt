package app.reseam.manager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.reseam.manager.ui.platform.rememberPackageAppIconPainter

private val PalettePool: List<List<Color>> = listOf(
    listOf(Color(0xFF5865F2), Color(0xFF454FBF)),
    listOf(Color(0xFFFF4500), Color(0xFFCC3700)),
    listOf(Color(0xFF1DB954), Color(0xFF0A7430)),
    listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)),
    listOf(Color(0xFFE50914), Color(0xFFB81D24)),
    listOf(Color(0xFF25D366), Color(0xFF128C7E)),
    listOf(Color(0xFFFF0000), Color(0xFFAA0000)),
    listOf(Color(0xFF0EA5E9), Color(0xFF1E3A8A)),
    listOf(Color(0xFF7C3AED), Color(0xFF4C1D95)),
)

@Composable
fun RsAppIcon(
    name: String?,
    modifier: Modifier = Modifier,
    packageName: String? = null,
    size: Dp = 48.dp,
) {
    val painter = rememberPackageAppIconPainter(packageName)
    val cornerRadius = size * 0.26f
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LetterTile(name = name, packageName = packageName, size = size)
        }
    }
}

@Composable
private fun LetterTile(name: String?, packageName: String?, size: Dp) {
    val key = remember(name, packageName) { (packageName ?: name ?: "").lowercase() }
    val palette = remember(key) {
        if (key.isEmpty()) PalettePool[0] else PalettePool[(key.hashCode().rem(PalettePool.size).let { if (it < 0) it + PalettePool.size else it })]
    }
    val initials = remember(name) {
        when {
            name.isNullOrBlank() -> "?"
            else -> name.split(' ', '_', '-')
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifEmpty { name.first().uppercase() }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                        endY = 8f,
                    ),
                ),
        )
    }
}
