package app.reseam.manager.ui.model

import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.patcher.InspectResponse
import app.reseam.manager.patcher.OptionKind
import app.reseam.manager.patcher.OptionMetadata
import app.reseam.manager.patcher.PatchMetadata
object PatchEditorFactory {
    fun create(appName: String?, inspect: InspectResponse): PatchEditorState {
        val patches = inspect.patches.map { patch ->
            PatchEditorItem(
                metadata = patch,
                enabled = patch.enabledByDefault && patch.isCompatible,
                required = patch.requiredByConvention(),
                options = patch.options.associate { option ->
                    option.key to PatchOptionEditorValue(
                        key = option.key,
                        kind = option.optionType,
                        value = option.defaultValue ?: option.emptyValue(),
                        validValues = option.validValues.orEmpty(),
                    )
                },
            )
        }

        return PatchEditorState(appName = appName, patches = patches)
    }

    private fun PatchMetadata.requiredByConvention(): Boolean =
        options.isEmpty() &&
            enabledByDefault &&
            name.contains("settings", ignoreCase = true)

    private fun OptionMetadata.emptyValue(): InputOptionValue =
        when (optionType) {
            OptionKind.String -> InputOptionValue.StringValue("")
            OptionKind.Bool -> InputOptionValue.BoolValue(false)
            OptionKind.Int -> InputOptionValue.IntValue(0)
            OptionKind.Float -> InputOptionValue.FloatValue(0.0)
            OptionKind.StringList -> InputOptionValue.StringListValue(emptyList())
            OptionKind.Path -> InputOptionValue.PathValue("")
        }
}
