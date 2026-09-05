package app.reseam.manager.ui.patches

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.reseam.manager.sdk.OptionDeclaration
import app.reseam.manager.sdk.OptionType
import app.reseam.manager.sdk.OptionValue
import app.reseam.manager.ui.components.ChoiceRow
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.components.Toggle
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun PatchOptionField(declaration: OptionDeclaration, value: OptionValue?, onChange: (OptionValue) -> Unit) {
    val colors = ReseamTheme.colors
    val choices = declaration.validValues.orEmpty()
    if (declaration.optionType == OptionType.Bool) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Toggle(checked = (value as? OptionValue.Bool)?.value == true, onCheckedChange = { onChange(OptionValue.Bool(it)) }, small = true)
            OptionLabel(declaration, Modifier.weight(1f))
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OptionLabel(declaration)
        when {
            declaration.optionType == OptionType.String && choices.isNotEmpty() -> ChoiceRow(
                choices = choices,
                selected = (value as? OptionValue.Text)?.value,
                onSelect = { onChange(OptionValue.Text(it)) },
            )
            declaration.optionType == OptionType.Int -> NumberField((value as? OptionValue.Int)?.value?.toString().orEmpty()) { text ->
                text.toLongOrNull()?.let { onChange(OptionValue.Int(it)) }
            }
            declaration.optionType == OptionType.Float -> NumberField((value as? OptionValue.Float)?.value?.toString().orEmpty()) { text ->
                text.toDoubleOrNull()?.let { onChange(OptionValue.Float(it)) }
            }
            declaration.optionType == OptionType.Path -> TextField(
                value = (value as? OptionValue.Path)?.value.orEmpty(),
                onValueChange = { onChange(OptionValue.Path(it)) },
                leading = Icons.Folder,
                mono = true,
            )
            declaration.optionType == OptionType.StringList -> TextField(
                value = (value as? OptionValue.TextList)?.value?.joinToString(", ").orEmpty(),
                onValueChange = { text -> onChange(OptionValue.TextList(text.split(',').map { it.trim() }.filter { it.isNotEmpty() })) },
                placeholder = "Comma-separated values",
            )
            else -> TextField(value = (value as? OptionValue.Text)?.value.orEmpty(), onValueChange = { onChange(OptionValue.Text(it)) })
        }
        if (declaration.description.isNotBlank()) {
            Text(declaration.description, style = ReseamTheme.typography.captionSmall, color = colors.mutedForeground)
        }
    }
}

@Composable
private fun OptionLabel(declaration: OptionDeclaration, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Text(
        text = if (declaration.required) "${declaration.title} *" else declaration.title,
        style = ReseamTheme.typography.captionMedium,
        color = colors.foreground,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onChange, mono = true, keyboardType = KeyboardType.Number)
}
