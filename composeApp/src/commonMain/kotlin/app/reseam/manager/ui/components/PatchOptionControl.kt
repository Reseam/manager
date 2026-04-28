package app.reseam.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.patcher.OptionKind
import app.reseam.manager.patcher.OptionMetadata
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.PatchOptionEditorValue
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun PatchOptionControl(
    meta: OptionMetadata,
    value: PatchOptionEditorValue?,
    onChange: (InputOptionValue) -> Unit,
) {
    val current = value?.value ?: meta.defaultValue
    val validValues = value?.validValues ?: meta.validValues.orEmpty()

    when {
        meta.optionType == OptionKind.Bool -> BoolOption(meta, current, onChange)
        meta.optionType == OptionKind.String && validValues.isNotEmpty() -> SelectOption(meta, current, validValues, onChange)
        meta.optionType == OptionKind.Path -> PathOption(meta, current, onChange)
        meta.optionType == OptionKind.Int || meta.optionType == OptionKind.Float -> NumberOption(meta, current, onChange)
        else -> TextOption(meta, current, onChange)
    }
}

@Composable
private fun BoolOption(
    meta: OptionMetadata,
    current: InputOptionValue?,
    onChange: (InputOptionValue) -> Unit,
) {
    val colors = ReseamTheme.colors
    val boolVal = (current as? InputOptionValue.BoolValue)?.value ?: false
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RsToggle(
            checked = boolVal,
            onCheckedChange = { onChange(InputOptionValue.BoolValue(it)) },
            size = RsButtonSize.Small,
        )
        Text(
            text = meta.title,
            style = ReseamTheme.typography.caption,
            color = colors.foreground,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SelectOption(
    meta: OptionMetadata,
    current: InputOptionValue?,
    validValues: List<String>,
    onChange: (InputOptionValue) -> Unit,
) {
    val colors = ReseamTheme.colors
    val currentValue = (current as? InputOptionValue.StringValue)?.value.orEmpty()
    OptionContainer(label = meta.title) {
        RsCard(
            modifier = Modifier.fillMaxWidth(),
            background = colors.background,
            borderColor = colors.borderStrong,
            cornerRadius = 10.dp,
            contentPadding = PaddingValues(3.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                validValues.forEach { option ->
                    SegmentChoice(
                        label = option,
                        selected = currentValue == option,
                        onClick = { onChange(InputOptionValue.StringValue(option)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PathOption(
    meta: OptionMetadata,
    current: InputOptionValue?,
    onChange: (InputOptionValue) -> Unit,
) {
    val colors = ReseamTheme.colors
    val currentValue = (current as? InputOptionValue.PathValue)?.value.orEmpty()
    OptionContainer(label = meta.title) {
        OptionInputBox {
            Icon(
                imageVector = ReseamIcons.Folder,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
            OptionTextField(
                value = currentValue,
                onValueChange = { onChange(InputOptionValue.PathValue(it)) },
                mono = true,
                modifier = Modifier.weight(1f),
            )
            RsCard(
                background = colors.mutedElevated,
                borderColor = null,
                cornerRadius = 6.dp,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Browse",
                    style = ReseamTheme.typography.captionSmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.foreground,
                )
            }
        }
    }
}

@Composable
private fun NumberOption(
    meta: OptionMetadata,
    current: InputOptionValue?,
    onChange: (InputOptionValue) -> Unit,
) {
    val text = when (current) {
        is InputOptionValue.IntValue -> current.value.toString()
        is InputOptionValue.FloatValue -> current.value.toString()
        else -> ""
    }
    OptionContainer(label = meta.title) {
        OptionInputBox {
            OptionTextField(
                value = text,
                onValueChange = { input ->
                    if (meta.optionType == OptionKind.Int) {
                        input.toLongOrNull()?.let { onChange(InputOptionValue.IntValue(it)) }
                    } else {
                        input.toDoubleOrNull()?.let { onChange(InputOptionValue.FloatValue(it)) }
                    }
                },
                mono = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TextOption(
    meta: OptionMetadata,
    current: InputOptionValue?,
    onChange: (InputOptionValue) -> Unit,
) {
    val colors = ReseamTheme.colors
    val text = when (current) {
        is InputOptionValue.StringValue -> current.value
        is InputOptionValue.PathValue -> current.value
        else -> ""
    }
    OptionContainer(label = meta.title) {
        RsCard(
            modifier = Modifier.fillMaxWidth(),
            background = colors.background,
            borderColor = colors.borderStrong,
            cornerRadius = 10.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        ) {
            OptionTextField(
                value = text,
                onValueChange = { onChange(InputOptionValue.StringValue(it)) },
                mono = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OptionContainer(
    label: String,
    content: @Composable () -> Unit,
) {
    val colors = ReseamTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = ReseamTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = colors.foreground,
        )
        content()
    }
}

@Composable
private fun OptionInputBox(content: @Composable RowScope.() -> Unit) {
    val colors = ReseamTheme.colors
    RsCard(
        modifier = Modifier.fillMaxWidth(),
        background = colors.background,
        borderColor = colors.borderStrong,
        cornerRadius = 10.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun OptionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    mono: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = ReseamTheme.colors
    val style = ReseamTheme.typography.caption.copy(
        color = colors.foreground,
        fontFamily = if (mono) ReseamTheme.typography.mono else ReseamTheme.typography.sans,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier,
    )
}

@Composable
private fun SegmentChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    RsCard(
        modifier = modifier.height(28.dp),
        background = if (selected) colors.accent else Color.Transparent,
        borderColor = null,
        cornerRadius = 8.dp,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ReseamTheme.typography.captionSmall.copy(fontWeight = FontWeight.Medium),
            color = if (selected) colors.foreground else colors.mutedForeground,
        )
    }
}
