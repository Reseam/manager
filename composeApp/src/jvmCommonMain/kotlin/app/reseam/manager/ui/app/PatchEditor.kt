package app.reseam.manager.ui.app

import app.reseam.manager.sdk.reference
import app.reseam.manager.sdk.supports
import app.reseam.manager.sdk.universal
import app.reseam.sdk.Compatibility
import app.reseam.sdk.OptionValue
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.PatchPreset
import app.reseam.sdk.PatchSelection

data class PatchRow(
    val meta: PatchMetadata,
    val compatible: Boolean,
    val enabled: Boolean,
    val options: Map<String, OptionValue>,
) {
    val reference: String get() = meta.reference
}

/** Mirrors the engine's `PatchSpec::in_preset`, which only answers for an inspected APK; patches are picked before the download. */
private fun PatchRow.enabledIn(preset: PatchPreset): Boolean = compatible && when (preset) {
    PatchPreset.RECOMMENDED -> meta.spec.enabledByDefault
    PatchPreset.ALL -> !meta.universal || meta.spec.enabledByDefault
    PatchPreset.NONE -> false
}

private fun PatchMetadata.compatibleWith(packageName: String, versionName: String?): Boolean = when (val compatibility = spec.compatibility) {
    Compatibility.Universal -> true
    is Compatibility.Packages -> compatibility.packages.any { it.`package` == packageName && (it.versions.isEmpty() || versionName in it.versions) }
}

data class PatchEditor(
    val rows: List<PatchRow>,
    val allowIncompatible: Boolean,
    private val chosen: PatchPreset? = null,
) {
    val enabled: List<PatchRow> get() = rows.filter { it.enabled }

    val forApp: List<PatchRow> get() = rows.filter { !it.meta.universal }

    fun selectable(row: PatchRow): Boolean = row.compatible || allowIncompatible

    fun toggle(reference: String, enabled: Boolean): PatchEditor {
        val affected = if (enabled) closure(reference) { it.meta.spec.dependencies } else dependents(reference)
        return copy(rows = rows.map { if (it.reference in affected && selectable(it)) it.copy(enabled = enabled) else it })
    }

    fun setOption(reference: String, key: String, value: OptionValue?): PatchEditor =
        copy(rows = rows.map { if (it.reference == reference) it.copy(options = if (value == null) it.options - key else it.options + (key to value)) else it })

    val preset: PatchPreset? get() = chosen?.takeIf { preset -> enabledBy(preset).let { enabled -> rows.all { it.enabled == it.reference in enabled } } }

    fun apply(preset: PatchPreset): PatchEditor = enabledBy(preset).let { enabled -> copy(chosen = preset, rows = rows.map { it.copy(enabled = it.reference in enabled) }) }

    fun allow(allowed: Boolean): PatchEditor =
        copy(allowIncompatible = allowed, rows = if (allowed) rows else rows.map { if (it.compatible) it else it.copy(enabled = false) })

    /** Nothing is disabled explicitly: that would block a dependency the engine has to run. */
    fun selection(): PatchSelection = PatchSelection(
        preset = PatchPreset.NONE,
        enable = enabled.map { it.reference },
        disable = emptyList(),
        options = enabled.filter { it.options.isNotEmpty() }.associate { it.reference to it.options },
        ignoreVersions = enabled.any { !it.compatible },
    )

    private fun enabledBy(preset: PatchPreset): Set<String> {
        val references = rows.filter { it.enabledIn(preset) }.flatMap { root -> closure(root.reference) { it.meta.spec.dependencies } }.toSet()
        return rows.filter { it.reference in references && selectable(it) }.map { it.reference }.toSet()
    }

    private fun dependents(reference: String): Set<String> =
        closure(reference) { row -> rows.filter { row.reference in it.meta.spec.dependencies }.map { it.reference } }

    private fun closure(start: String, next: (PatchRow) -> List<String>): Set<String> {
        val byReference = rows.associateBy { it.reference }
        val seen = linkedSetOf<String>()
        val pending = ArrayDeque(listOf(start))
        while (pending.isNotEmpty()) {
            val row = byReference[pending.removeFirst()] ?: continue
            if (seen.add(row.reference)) pending += next(row)
        }
        return seen
    }

    companion object {
        fun from(patches: List<PatchMetadata>, packageName: String, versionName: String?, allowIncompatible: Boolean): PatchEditor = PatchEditor(
            rows = patches.filter { !it.spec.hidden && it.supports(packageName) }
                .sortedBy { it.universal }
                .map { meta ->
                    PatchRow(
                        meta = meta,
                        compatible = meta.compatibleWith(packageName, versionName),
                        enabled = false,
                        options = meta.spec.options.mapNotNull { option -> option.defaultValue?.let { option.key to it } }.toMap(),
                    )
                },
            allowIncompatible = allowIncompatible,
        ).apply(PatchPreset.RECOMMENDED)
    }
}
