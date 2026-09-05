package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leading: ImageVector? = null,
    mono: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailing: @Composable () -> Unit = {},
) {
    val colors = ReseamTheme.colors
    val shape = ReseamTheme.shapes.medium
    val style = (if (mono) ReseamTheme.typography.mono else ReseamTheme.typography.bodySmall).copy(color = colors.foreground)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(colors.surfaceSunken, shape)
            .border(1.dp, colors.borderStrong, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) Icon(leading, null, tint = colors.mutedForeground, modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = style, color = colors.subtleForeground)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = style,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing()
    }
}
