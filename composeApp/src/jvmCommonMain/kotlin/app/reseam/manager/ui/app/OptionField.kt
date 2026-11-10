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
import app.reseam.sdk.OptionProblem
import app.reseam.sdk.OptionText
import app.reseam.sdk.OptionType
import app.reseam.sdk.OptionValue
import app.reseam.sdk.optionText
import app.reseam.sdk.parseOption
import org.jetbrains.compose.resources.stringResource

@Composable
fun OptionField(option: OptionDeclaration, text: OptionText, value: OptionValue?, onChange: (OptionValue?) -> Unit) {
    val choices = option.validValues
    when {
        option.optionType == OptionType.BOOL -> Row(
            modifier = Modifier.fillMaxWidth().toggleable((value as? OptionValue.Bool)?.field0 == true, role = Role.Switch) { onChange(OptionValue.Bool(it)) },
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemText(text.title, text.description.ifEmpty { null }, Modifier.weight(1f))
            Switch(checked = (value as? OptionValue.Bool)?.field0 == true, onCheckedChange = null)
        }
        choices != null -> Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            ItemText(text.title, text.description.ifEmpty { null })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                choices.forEach { choice ->
                    Chip(choice, outlined = value?.let(::optionText) != choice, onClick = { onChange(option.read(choice).value) })
                }
            }
        }
        else -> {
            var text by remember(option.key) { mutableStateOf(value?.let(::optionText).orEmpty()) }
            val problem = text.takeIf { it.isNotBlank() }?.let { option.read(it).problem }
            TextField(
                value = text,
                onValueChange = { next ->
                    text = next
                    if (next.isBlank()) {
                        onChange(null)
                    } else {
                        option.read(next).value?.let(onChange)
                    }
                },
                label = option.title,
                modifier = Modifier.fillMaxWidth(),
                supporting = option.description.ifEmpty { null },
                error = problem?.text(),
                keyboardType = if (option.optionType == OptionType.INT || option.optionType == OptionType.FLOAT) KeyboardType.Decimal else KeyboardType.Text,
                singleLine = option.optionType != OptionType.STRING_LIST,
            )
        }
    }
}

/** [text] read as a value of this option, or why it isn't one. */
private class Reading(val value: OptionValue?, val problem: OptionProblem?)

private fun OptionDeclaration.read(text: String): Reading = try {
    Reading(parseOption(this, text), null)
} catch (problem: OptionProblem) {
    Reading(null, problem)
}

@Composable
private fun OptionProblem.text(): String = when (this) {
    OptionProblem.Missing -> stringResource(Res.string.option_missing)
    OptionProblem.NotABool -> stringResource(Res.string.option_not_a_bool)
    OptionProblem.NotAnInteger -> stringResource(Res.string.option_not_whole)
    OptionProblem.NotANumber -> stringResource(Res.string.option_not_a_number)
    is OptionProblem.NotAllowed -> stringResource(Res.string.option_not_allowed, allowed.joinToString())
    OptionProblem.Undeclared, is OptionProblem.WrongType -> stringResource(Res.string.option_outdated)
}
