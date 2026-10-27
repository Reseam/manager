package app.reseam.manager.ui.app

import app.reseam.manager.sdk.reference
import app.reseam.manager.sdk.supports
import app.reseam.manager.sdk.text
import app.reseam.manager.sdk.universal
import app.reseam.sdk.Compatibility
import app.reseam.sdk.OptionDeclaration
import app.reseam.sdk.OptionValue
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.PatchPreset
import app.reseam.sdk.PatchRef
import app.reseam.sdk.PatchSelection

data class PatchRow(
    val meta: PatchMetadata,
    val compatible: Boolean,
    val enabled: Boolean,
    val options: Map<String, OptionValue>,
) {
    val reference: PatchRef get() = meta.reference

    val missingOptions: List<OptionDeclaration> get() = meta.spec.options.filter { it.required && it.key !in options }
}

data class PatchCount(val universal: Boolean, val selected: Int, val total: Int)

/** Mirrors the engine's `PatchSpec::in_preset`, which only answers for an inspected APK; patches are picked before the download. */
private fun PatchRow.enabledIn(preset: PatchPreset): Boolean = when (preset) {
    PatchPreset.RECOMMENDED -> meta.spec.enabledByDefault
    PatchPreset.ALL -> !meta.universal || meta.spec.enabledByDefault
    PatchPreset.NONE -> false
}

private fun PatchMetadata.compatibleWith(packageName: String, versionName: String?): Boolean = when (val compatibility = spec.compatibility) {
    Compatibility.Universal -> true
    is Compatibility.Packages -> compatibility.packages.any { it.`package` == packageName && (it.versions.isEmpty() || versionName in it.versions) }
}

private fun List<PatchRow>.tally(universal: Boolean): PatchCount = PatchCount(universal, count { it.enabled }, size)

data class PatchEditor(
    val rows: List<PatchRow>,
    val allowIncompatible: Boolean,
    private val chosen: PatchPreset? = null,
) {
    val enabled: List<PatchRow> get() = rows.filter { it.enabled }

    val incomplete: List<PatchRow> get() = enabled.filter { it.missingOptions.isNotEmpty() }

    val runnable: Boolean get() = enabled.isNotEmpty() && incomplete.isEmpty()

    val counts: List<PatchCount>
        get() {
            val (universal, own) = rows.partition { it.meta.universal }
            return listOfNotNull(
                own.tally(universal = false).takeIf { own.isNotEmpty() },
                universal.tally(universal = true).takeIf { own.isEmpty() || it.selected > 0 },
            )
        }

    fun selectable(row: PatchRow): Boolean = row.compatible || allowIncompatible

    fun requiredBy(row: PatchRow): List<PatchRow> = enabled.filter { row.reference in it.meta.spec.dependencies }

    fun toggle(reference: PatchRef, enabled: Boolean): PatchEditor {
        val affected = if (enabled) closure(reference) { it.meta.spec.dependencies } else dependents(reference)
        return copy(rows = rows.map { if (it.reference in affected && selectable(it)) it.copy(enabled = enabled) else it })
    }

    fun setOption(reference: PatchRef, key: String, value: OptionValue?): PatchEditor =
        copy(rows = rows.map { if (it.reference == reference) it.copy(options = if (value == null) it.options - key else it.options + (key to value)) else it })

    val preset: PatchPreset? get() = chosen?.takeIf { preset -> enabledBy(preset).let { enabled -> rows.all { it.enabled == it.reference in enabled } } }

    fun apply(preset: PatchPreset): PatchEditor = enabledBy(preset).let { enabled -> copy(chosen = preset, rows = rows.map { it.copy(enabled = it.reference in enabled) }) }

    fun allow(allowed: Boolean): PatchEditor {
        val kept = preset
        val next = copy(allowIncompatible = allowed, rows = if (allowed) rows else rows.map { if (it.compatible) it else it.copy(enabled = false) })
        return kept?.let(next::apply) ?: next
    }

    /** Nothing is disabled explicitly: that would block a dependency the engine has to run. */
    fun selection(): PatchSelection = PatchSelection(
        preset = PatchPreset.NONE,
        enable = enabled.map { it.reference.text },
        disable = emptyList(),
        options = enabled.filter { it.options.isNotEmpty() }.associate { it.reference.text to it.options },
        ignoreVersions = enabled.any { !it.compatible },
    )

    private fun enabledBy(preset: PatchPreset): Set<PatchRef> {
        val byReference = rows.associateBy { it.reference }
        val untestedVersion = allowIncompatible && rows.none { !it.meta.universal && it.compatible }
        return rows.filter { (it.compatible || untestedVersion) && it.enabledIn(preset) }
            .map { root -> closure(root.reference) { it.meta.spec.dependencies } }
            .filter { patches -> patches.all { byReference.getValue(it).missingOptions.isEmpty() } }
            .flatten()
            .filter { selectable(byReference.getValue(it)) }
            .toSet()
    }

    private fun dependents(reference: PatchRef): Set<PatchRef> =
        closure(reference) { row -> rows.filter { row.reference in it.meta.spec.dependencies }.map { it.reference } }

    private fun closure(start: PatchRef, next: (PatchRow) -> List<PatchRef>): Set<PatchRef> {
        val byReference = rows.associateBy { it.reference }
        val seen = linkedSetOf<PatchRef>()
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
