package app.reseam.manager.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ItemText
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.theme.Space
import app.reseam.sdk.OptionDeclaration
import app.reseam.sdk.OptionType
import app.reseam.sdk.OptionValue
import org.jetbrains.compose.resources.stringResource

@Composable
fun OptionField(option: OptionDeclaration, value: OptionValue?, onChange: (OptionValue?) -> Unit) {
    val choices = option.validValues
    when {
        option.optionType == OptionType.BOOL -> Row(
            modifier = Modifier.fillMaxWidth().toggleable((value as? OptionValue.Bool)?.field0 == true, role = Role.Switch) { onChange(OptionValue.Bool(it)) },
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemText(option.title, option.description.ifEmpty { null }, Modifier.weight(1f))
            Switch(checked = (value as? OptionValue.Bool)?.field0 == true, onCheckedChange = null)
        }
        choices != null -> Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            ItemText(option.title, option.description.ifEmpty { null })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                choices.forEach { choice ->
                    Chip(choice, outlined = value?.text() != choice, onClick = { onChange(option.optionType.parse(choice)) })
                }
            }
        }
        else -> {
            var text by remember(option.key) { mutableStateOf(value?.text().orEmpty()) }
            val parsed = text.takeIf { it.isNotBlank() }?.let(option.optionType::parse)
            TextField(
                value = text,
                onValueChange = { next ->
                    text = next
                    val nextValue = next.takeIf { it.isNotBlank() }?.let(option.optionType::parse)
                    if (next.isBlank() || nextValue != null) onChange(nextValue)
                },
                label = option.title,
                modifier = Modifier.fillMaxWidth(),
                supporting = option.description.ifEmpty { null },
                error = stringResource(Res.string.option_not_a_number).takeIf { text.isNotBlank() && parsed == null },
                keyboardType = if (option.optionType == OptionType.INT || option.optionType == OptionType.FLOAT) KeyboardType.Decimal else KeyboardType.Text,
                singleLine = option.optionType != OptionType.STRING_LIST,
            )
        }
    }
}

private fun OptionValue.text(): String = when (this) {
    is OptionValue.Text -> field0
    is OptionValue.Bool -> field0.toString()
    is OptionValue.Int -> field0.toString()
    is OptionValue.Float -> field0.toString()
    is OptionValue.TextList -> field0.joinToString("\n")
    is OptionValue.Path -> field0
}

private fun OptionType.parse(text: String): OptionValue? = when (this) {
    OptionType.STRING -> OptionValue.Text(text)
    OptionType.BOOL -> text.toBooleanStrictOrNull()?.let(OptionValue::Bool)
    OptionType.INT -> text.trim().toLongOrNull()?.let(OptionValue::Int)
    OptionType.FLOAT -> text.trim().toDoubleOrNull()?.let(OptionValue::Float)
    OptionType.STRING_LIST -> OptionValue.TextList(text.lines().map(String::trim).filter(String::isNotEmpty))
    OptionType.PATH -> OptionValue.Path(text)
}
