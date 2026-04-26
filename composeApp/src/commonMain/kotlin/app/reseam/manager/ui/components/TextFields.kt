package app.reseam.manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
) {
    val colors = ReseamTheme.colors
    val textStyle = ReseamTheme.typography.bodySmall.copy(color = colors.foreground)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(colors.cardElevated, RoundedCornerShape(12.dp))
            .border(1.dp, colors.borderStrong.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) {
            Icon(
                imageVector = leading,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = textStyle.copy(color = colors.mutedForeground),
                )
            }
        }
    }
}

@Composable
fun RsValueField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    style: TextStyle = ReseamTheme.typography.caption,
    leading: ImageVector? = null,
    trailingButtonLabel: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null,
) {
    val colors = ReseamTheme.colors
    val textStyle = style.copy(color = colors.foreground)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(colors.background, RoundedCornerShape(10.dp))
            .border(1.dp, colors.borderStrong.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading != null) {
            Icon(
                imageVector = leading,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
            if (value.isEmpty() && placeholder != null) {
                Text(text = placeholder, style = textStyle.copy(color = colors.mutedForeground))
            }
        }
        if (suffix != null) {
            Text(
                text = suffix,
                style = ReseamTheme.typography.captionSmall.copy(color = colors.mutedForeground, fontFamily = ReseamTheme.typography.mono),
            )
        }
        if (trailingButtonLabel != null && onTrailingClick != null) {
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .background(colors.mutedElevated, RoundedCornerShape(6.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(6.dp))
                    .clickable(onClick = onTrailingClick)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = trailingButtonLabel,
                    style = ReseamTheme.typography.captionSmall,
                    color = colors.foreground,
                )
            }
        }
    }
}

