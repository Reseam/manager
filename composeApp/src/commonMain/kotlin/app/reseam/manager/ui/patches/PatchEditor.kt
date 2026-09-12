package app.reseam.manager.ui.patches

import app.reseam.manager.sdk.InspectResponse
import app.reseam.manager.sdk.OptionValue
import app.reseam.manager.sdk.PatchMetadata
import app.reseam.manager.sdk.PatchSelection
import app.reseam.manager.sdk.emptyValue

data class PatchRow(
    val meta: PatchMetadata,
    val enabled: Boolean,
    val options: Map<String, OptionValue>,
) {
    val id: String get() = meta.id
    val compatible: Boolean get() = meta.incompatibility == null
    val universal: Boolean get() = meta.universal
}

/**
 * The user's choices over one inspect result. Enabling a patch enables its dependencies; disabling one disables its dependents.
 * Rows the engine marked incompatible only follow the user's toggles while [allowIncompatible] is set.
 */
data class PatchEditor(val rows: List<PatchRow>, val selected: String? = null, val allowIncompatible: Boolean = false) {
    val enabledCount: Int get() = rows.count { it.enabled }
    val incompatibleCount: Int get() = rows.count { !it.compatible }
    val enabledIncompatibleCount: Int get() = rows.count { it.enabled && !it.compatible }

    fun selectable(row: PatchRow): Boolean = row.compatible || allowIncompatible

    fun toggle(id: String, enabled: Boolean): PatchEditor {
        val affected = if (enabled) closure(id) { it.meta.dependencies } else dependents(id)
        return copy(rows = rows.map { if (it.id in affected && selectable(it)) it.copy(enabled = enabled) else it })
    }

    /**
     * The enabled rows that pull [id] in. They are why it is on, and why switching it off
     * switches them off too.
     */
    fun requiredBy(id: String): List<PatchRow> =
        (dependents(id) - id).mapNotNull { dependent -> rows.find { it.id == dependent } }.filter { it.enabled }

    fun setOption(id: String, key: String, value: OptionValue): PatchEditor =
        copy(rows = rows.map { if (it.id == id) it.copy(options = it.options + (key to value)) else it })

    fun select(id: String?): PatchEditor = copy(selected = id)

    /** Every compatible patch on; untested ones stay as they are, a bulk action must not pull them in. */
    fun enableAll(): PatchEditor = copy(rows = rows.map { if (it.compatible) it.copy(enabled = true) else it })

    /** Withdrawing the allowance switches untested patches off; they cannot run without it. */
    fun allow(allowed: Boolean): PatchEditor =
        copy(allowIncompatible = allowed, rows = if (allowed) rows else rows.map { if (it.compatible) it else it.copy(enabled = false) })

    fun selection(): PatchSelection = PatchSelection(
        enable = rows.filter { it.enabled }.map { it.id }.toSet(),
        disable = rows.filterNot { it.enabled }.map { it.id }.toSet(),
        options = rows.filter { it.enabled && it.options.isNotEmpty() }.associate { it.id to it.options },
        ignoreVersions = enabledIncompatibleCount > 0,
    )

    fun queue(): List<String> = rows.filter { it.enabled }.map { it.id }

    /** [id] and everything that depends on it, however deep the chain runs. */
    private fun dependents(id: String): Set<String> =
        closure(id) { row -> rows.filter { row.id in it.meta.dependencies }.map { it.id } }

    private fun closure(start: String, next: (PatchRow) -> List<String>): Set<String> {
        val byId = rows.associateBy { it.id }
        val seen = linkedSetOf<String>()
        val pending = ArrayDeque(listOf(start))
        while (pending.isNotEmpty()) {
            val row = byId[pending.removeFirst()] ?: continue
            if (!seen.add(row.id)) continue
            pending += next(row)
        }
        return seen
    }

    companion object {
        fun from(response: InspectResponse, packageName: String?, allowIncompatible: Boolean): PatchEditor {
            val patches = response.patches.filter { !it.hidden && (packageName == null || it.supports(packageName)) }
            return PatchEditor(
                rows = patches.map { meta ->
                    PatchRow(
                        meta = meta,
                        enabled = meta.enabledByDefault && meta.incompatibility == null,
                        options = meta.options.associate { it.key to (it.defaultValue ?: it.optionType.emptyValue()) },
                    )
                },
                allowIncompatible = allowIncompatible,
            )
        }
    }
}
