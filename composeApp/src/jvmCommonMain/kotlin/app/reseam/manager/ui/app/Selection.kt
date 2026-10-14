package app.reseam.manager.ui.app

import androidx.compose.runtime.Composable
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.NoteTone
import app.reseam.manager.ui.components.RowNote
import app.reseam.sdk.PatchPreset
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun patchCounts(editor: PatchEditor): String = editor.counts
    .map { pluralStringResource(if (it.universal) Res.plurals.patch_count_universal else Res.plurals.patch_count, it.total, it.selected, it.total) }
    .reduceOrNull { first, second -> stringResource(Res.string.joined, first, second) }
    .orEmpty()

@Composable
fun presetLabel(preset: PatchPreset?): String = stringResource(
    when (preset) {
        PatchPreset.RECOMMENDED -> Res.string.preset_recommended
        PatchPreset.ALL -> Res.string.preset_all
        PatchPreset.NONE -> Res.string.preset_none
        null -> Res.string.preset_custom
    },
)

@Composable
fun missingOptionsNote(row: PatchRow): RowNote =
    RowNote(stringResource(Res.string.patch_options_missing, row.meta.spec.name, row.missingOptions.joinToString { it.title }), NoteTone.Error)

@Composable
fun rowNotes(row: PatchRow, editor: PatchEditor, versionName: String?): List<RowNote> = listOfNotNull(
    untestedNote(row, editor.allowIncompatible, versionName),
    requiredNote(editor.requiredBy(row)),
    if (row.enabled && row.missingOptions.isNotEmpty()) missingOptionsNote(row) else null,
)

@Composable
private fun untestedNote(row: PatchRow, allowed: Boolean, versionName: String?): RowNote? = when {
    row.compatible -> null
    allowed -> RowNote(versionName?.let { stringResource(Res.string.patches_untested_allowed, it) } ?: stringResource(Res.string.patches_untested_newest_allowed), NoteTone.Caution)
    else -> RowNote(versionName?.let { stringResource(Res.string.patches_untested, it) } ?: stringResource(Res.string.patches_untested_newest), NoteTone.Error)
}

@Composable
private fun requiredNote(dependents: List<PatchRow>): RowNote? {
    val first = dependents.firstOrNull()?.meta?.spec?.name ?: return null
    val text = if (dependents.size == 1) {
        stringResource(Res.string.patches_required_by, first)
    } else {
        pluralStringResource(Res.plurals.patches_required_by_more, dependents.size - 1, dependents.size - 1, first)
    }
    return RowNote(text, NoteTone.Info)
}
