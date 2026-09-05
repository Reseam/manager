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
}

/** The user's choices over one inspect result. Enabling a patch enables its dependencies; disabling one disables its dependents. */
data class PatchEditor(val rows: List<PatchRow>, val expanded: String? = null) {
    val enabledCount: Int get() = rows.count { it.enabled }

    fun toggle(id: String, enabled: Boolean): PatchEditor {
        val affected = if (enabled) closure(id) { it.meta.dependencies } else closure(id) { row -> rows.filter { id in it.meta.dependencies }.map { it.id } }
        return copy(rows = rows.map { if (it.id in affected && it.compatible) it.copy(enabled = enabled) else it })
    }

    fun setOption(id: String, key: String, value: OptionValue): PatchEditor =
        copy(rows = rows.map { if (it.id == id) it.copy(options = it.options + (key to value)) else it })

    fun expand(id: String?): PatchEditor = copy(expanded = id)

    fun enableAll(): PatchEditor = copy(rows = rows.map { it.copy(enabled = it.compatible) })

    fun selection(): PatchSelection = PatchSelection(
        enable = rows.filter { it.enabled }.map { it.id }.toSet(),
        disable = rows.filterNot { it.enabled }.map { it.id }.toSet(),
        options = rows.filter { it.enabled && it.options.isNotEmpty() }.associate { it.id to it.options },
    )

    fun queue(): List<String> = rows.filter { it.enabled }.map { it.id }

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
        fun from(response: InspectResponse, packageName: String?): PatchEditor {
            val patches = response.patches.filter { packageName == null || it.supports(packageName) }
            return PatchEditor(
                rows = patches.map { meta ->
                    PatchRow(
                        meta = meta,
                        enabled = meta.enabledByDefault && meta.incompatibility == null,
                        options = meta.options.associate { it.key to (it.defaultValue ?: it.optionType.emptyValue()) },
                    )
                },
            )
        }
    }
}
