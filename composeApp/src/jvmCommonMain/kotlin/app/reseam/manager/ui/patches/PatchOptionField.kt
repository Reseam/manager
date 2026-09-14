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
import app.reseam.manager.ui.components.ChoiceRow
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.components.Toggle
import app.reseam.manager.ui.theme.ReseamTheme
import app.reseam.sdk.OptionDeclaration
import app.reseam.sdk.OptionType
import app.reseam.sdk.OptionValue

@Composable
fun PatchOptionField(declaration: OptionDeclaration, value: OptionValue?, onChange: (OptionValue) -> Unit) {
    val colors = ReseamTheme.colors
    val choices = declaration.validValues.orEmpty()
    if (declaration.optionType == OptionType.BOOL) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionLabel(declaration, Modifier.weight(1f))
                Toggle(checked = (value as? OptionValue.Bool)?.field0 == true, onCheckedChange = { onChange(OptionValue.Bool(it)) }, small = true)
            }
            if (declaration.description.isNotBlank()) {
                Text(declaration.description, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
            }
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OptionLabel(declaration)
        when {
            declaration.optionType == OptionType.STRING && choices.isNotEmpty() -> ChoiceRow(
                choices = choices,
                selected = (value as? OptionValue.Text)?.field0,
                onSelect = { onChange(OptionValue.Text(it)) },
            )
            declaration.optionType == OptionType.INT -> NumberField((value as? OptionValue.Int)?.field0?.toString().orEmpty()) { text ->
                text.toLongOrNull()?.let { onChange(OptionValue.Int(it)) }
            }
            declaration.optionType == OptionType.FLOAT -> NumberField((value as? OptionValue.Float)?.field0?.toString().orEmpty()) { text ->
                text.toDoubleOrNull()?.let { onChange(OptionValue.Float(it)) }
            }
            declaration.optionType == OptionType.PATH -> TextField(
                value = (value as? OptionValue.Path)?.field0.orEmpty(),
                onValueChange = { onChange(OptionValue.Path(it)) },
                leading = Icons.Folder,
                mono = true,
            )
            declaration.optionType == OptionType.STRING_LIST -> TextField(
                value = (value as? OptionValue.TextList)?.field0?.joinToString(", ").orEmpty(),
                onValueChange = { text -> onChange(OptionValue.TextList(text.split(',').map { it.trim() }.filter { it.isNotEmpty() })) },
                placeholder = "Comma-separated values",
            )
            else -> TextField(value = (value as? OptionValue.Text)?.field0.orEmpty(), onValueChange = { onChange(OptionValue.Text(it)) })
        }
        if (declaration.description.isNotBlank()) {
            Text(declaration.description, style = ReseamTheme.typography.caption, color = colors.mutedForeground)
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
        modifier = modifier,
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit) {
    TextField(value = value, onValueChange = onChange, mono = true, keyboardType = KeyboardType.Number)
}
