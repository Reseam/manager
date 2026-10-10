package app.reseam.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.Borders
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space

@Composable
fun StateMessage(icon: ImageVector, title: String, body: String?, modifier: Modifier = Modifier, action: DialogAction? = null) {
    Box(modifier.fillMaxSize().padding(Space.xxl), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.iconLg))
            ItemText(title, body, titleStyle = MaterialTheme.typography.titleMedium, centered = true)
            if (action != null) Button(action.label, action.onClick, style = ButtonStyle.Tonal, size = ButtonSize.Medium)
        }
    }
}

@Composable
fun Spinner(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    CircularProgressIndicator(modifier.size(Sizes.iconMd), color = color, strokeWidth = Borders.thick)
}

@Composable
fun Loading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
