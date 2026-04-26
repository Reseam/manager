package app.reseam.manager.ui.model

import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.patcher.InspectResponse
import app.reseam.manager.patcher.OptionKind
import app.reseam.manager.patcher.OptionMetadata
import app.reseam.manager.patcher.PatchMetadata

object PatchEditorFactory {
    fun create(
        appName: String?,
        inspect: InspectResponse,
        packageName: String? = null,
        cachedPatches: List<PatchMetadata> = emptyList(),
    ): PatchEditorState {
        val source = when {
            cachedPatches.isNotEmpty() -> {
                val live = inspect.patches.associateBy { it.name }
                cachedPatches.map { cached ->
                    val match = live[cached.name]
                    cached.copy(
                        isCompatible = match?.isCompatible ?: false,
                        incompatibilityReason = match?.incompatibilityReason,
                    )
                }
            }
            packageName != null -> inspect.patches.filter { patch ->
                patch.compatibleWith.isEmpty() ||
                    patch.compatibleWith.any { it.packageName == packageName }
            }
            else -> inspect.patches
        }

        val patches = source.map { patch ->
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
