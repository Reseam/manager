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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
            .height(48.dp)
            .background(colors.surfaceSunken, shape)
            .border(1.dp, colors.borderStrong, shape)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leading != null) Icon(leading, null, tint = colors.mutedForeground, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = style, color = colors.subtleForeground)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = style,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (keyboardType == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing()
    }
}

/** A masked field with a reveal toggle. */
@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "Password") {
    var visible by rememberSaveable { mutableStateOf(false) }
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leading = Icons.Lock,
        keyboardType = if (visible) KeyboardType.Text else KeyboardType.Password,
        trailing = {
            IconButton(
                icon = if (visible) Icons.EyeOff else Icons.Eye,
                contentDescription = if (visible) "Hide password" else "Show password",
                onClick = { visible = !visible },
                size = 36.dp,
            )
        },
    )
}
