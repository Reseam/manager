package app.reseam.manager

import app.reseam.manager.data.PatchedApp
import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.parse
import app.reseam.manager.sdk.reference
import app.reseam.manager.sdk.text
import app.reseam.manager.sdk.universal
import app.reseam.sdk.AppIdentity
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.OptionDeclaration
import app.reseam.sdk.OptionValue
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.PatchPreset
import app.reseam.sdk.PatchRef
import app.reseam.sdk.PatchSelection
import app.reseam.sdk.PatchText
import app.reseam.sdk.PlanRequest
import app.reseam.sdk.PlannedPatch
import app.reseam.sdk.PlannedRun
import app.reseam.sdk.SdkError
import app.reseam.sdk.SelectionPlan
import app.reseam.sdk.plan

data class PatchRow(
    val meta: PatchMetadata,
    val planned: PlannedPatch,
    /** The values the patch runs with: the user's, then the patch's defaults. */
    val options: Map<String, OptionValue>,
) {
    val reference: PatchRef get() = planned.patch

    val text: PatchText by lazy { meta.text() }

    val compatible: Boolean get() = planned.incompatibility == null

    /** The run will execute the patch, chosen or needed by a chosen one. */
    val enabled: Boolean get() = planned.run == PlannedRun.Runs || planned.run is PlannedRun.Unmountable

    /** The options the patch needs a valid value for before it can run. */
    val neededOptions: List<OptionDeclaration>
        get() = planned.optionProblems.mapNotNull { issue -> meta.spec.options.firstOrNull { it.key == issue.key } }
}

data class PatchCount(val universal: Boolean, val selected: Int, val total: Int)

/** A patch source with patches for the app, and whether the app's builds use it. */
data class PatchSource(val id: String, val name: String, val used: Boolean)

/**
 * The user's patch choice for one app: a preset with the patches turned on or off, and the option values the user set.
 * The SDK plans what that choice runs, so the rows show what a run would do. Saving the choice rather than the patches
 * it ran lets patching again take up patches new to the preset.
 */
class PatchEditor private constructor(
    private val bundles: Map<String, List<PatchMetadata>>,
    /** The sources the user left out of the app's builds, by id. */
    val unused: Set<String>,
    private val app: AppIdentity,
    private val installMethod: InstallMethod,
    private val choice: PatchSelection,
    private val values: Map<PatchRef, Map<String, OptionValue>>,
) {
    /** The patches of the sources the app's builds use. */
    val patches: List<PatchMetadata> = bundles.filterKeys { it !in unused }.values.flatten()

    private val planned: SelectionPlan?

    /** Why the SDK cannot plan the choice, such as two sources declaring one patch. The editor then has no rows. */
    val failure: SdkError?

    init {
        // The SDK ignores the values of patches the choice leaves off, so they stay set while the user turns a patch off.
        val request = PlanRequest(patches, app, choice.copy(options = values.mapKeys { it.key.text }), installMethod)
        val (planned, failure) = try {
            plan(request) to null
        } catch (error: SdkError) {
            null to error
        }
        this.planned = planned
        this.failure = failure
    }

    val rows: List<PatchRow> = run {
        val metadata = patches.associateBy { it.reference }
        planned?.patches.orEmpty().mapNotNull { patch ->
            val meta = metadata.getValue(patch.patch)
            if (meta.spec.hidden) return@mapNotNull null
            val defaults = meta.spec.options.mapNotNull { option -> option.defaultValue?.let { option.key to it } }.toMap()
            PatchRow(meta, patch, defaults + values[patch.patch].orEmpty())
        }.sortedBy { it.meta.universal }
    }

    val sources: List<PatchSource> = bundles.mapNotNull { (id, patches) ->
        patches.firstOrNull { !it.spec.hidden && (it.universal || it.declared.any { declared -> declared.`package` == app.packageName }) }
            ?.let { PatchSource(id, it.spec.bundle, id !in unused) }
    }

    val allSourcesOff: Boolean get() = sources.isNotEmpty() && sources.none { it.used }

    /** The choice without entries that name no patch here, such as those of sources the app leaves out, which a run fails on. */
    private val known: PatchSelection = planned?.unknown.orEmpty().toSet().let { unknown -> choice.copy(enable = choice.enable - unknown, disable = choice.disable - unknown) }

    val allowIncompatible: Boolean get() = choice.ignoreVersions

    val enabled: List<PatchRow> get() = rows.filter { it.enabled }

    val incomplete: List<PatchRow> get() = rows.filter { it.planned.optionProblems.isNotEmpty() }

    val runnable: Boolean get() = enabled.isNotEmpty() && incomplete.isEmpty()

    val counts: List<PatchCount>
        get() {
            val (universal, own) = rows.partition { it.meta.universal }
            return listOfNotNull(
                own.tally(universal = false).takeIf { own.isNotEmpty() },
                universal.tally(universal = true).takeIf { own.isEmpty() || it.selected > 0 },
            )
        }

    /** The preset the choice is, or none when the user changed it. */
    val preset: PatchPreset? get() = known.preset.takeIf { known.enable.isEmpty() && known.disable.isEmpty() }

    /** The preset the patches of the source named [bundle] are chosen as, or none when the user changed them. */
    fun preset(bundle: String): PatchPreset? {
        val own = rows.filter { it.reference.bundle == bundle }
        return PatchPreset.entries.firstOrNull { preset -> own.all { chosen(it) == preset in it.planned.presets } }
    }

    fun selectable(row: PatchRow): Boolean = row.compatible || allowIncompatible

    /** The rows of the chosen patches that need [row]. */
    fun requiredBy(row: PatchRow): List<PatchRow> = rows.filter { it.reference in row.planned.requiredBy }

    /**
     * Turning a patch on also lifts the turn-off of the patches it needs, which the run would otherwise refuse it for;
     * turning it off also turns off the patches that need it.
     */
    fun toggle(reference: PatchRef, enabled: Boolean): PatchEditor {
        val byPreset = rows.filter { choice.preset in it.planned.presets }.map { it.reference.text }.toSet()
        if (enabled) {
            val enable = choice.enable + listOfNotNull(reference.text.takeIf { it !in byPreset })
            return choose(choice.copy(enable = enable.distinct(), disable = choice.disable - needs(reference).map { it.text }.toSet()))
        }
        val texts = dependents(reference).map { it.text }.toSet()
        return choose(choice.copy(enable = choice.enable - texts, disable = (choice.disable + texts.filter { it in byPreset }).distinct()))
    }

    fun setOption(reference: PatchRef, key: String, value: OptionValue?): PatchEditor {
        val patch = values[reference].orEmpty().let { if (value == null) it - key else it + (key to value) }
        return PatchEditor(bundles, unused, app, installMethod, choice, (values - reference) + listOfNotNull(patch.takeIf { it.isNotEmpty() }?.let { reference to it }))
    }

    fun apply(preset: PatchPreset): PatchEditor = choose(choice.copy(preset = preset, enable = emptyList(), disable = emptyList()))

    /** [preset] for the patches of the source named [bundle]; the other sources keep their choice. */
    fun apply(preset: PatchPreset, bundle: String): PatchEditor {
        val own = rows.filter { it.reference.bundle == bundle }
        val texts = own.map { it.reference.text }.toSet()
        val wanted = own.filter { preset in it.planned.presets }.map { it.reference.text }.toSet()
        val byPreset = own.filter { choice.preset in it.planned.presets }.map { it.reference.text }.toSet()
        return choose(choice.copy(enable = choice.enable - texts + (wanted - byPreset), disable = choice.disable - texts + (byPreset - wanted)))
    }

    /** A source the app leaves out keeps its patches in the choice, so turning it back on restores them. */
    fun use(source: String, used: Boolean): PatchEditor =
        PatchEditor(bundles, if (used) unused - source else unused + source, app, installMethod, choice, values)

    fun allow(allowed: Boolean): PatchEditor = choose(choice.copy(ignoreVersions = allowed))

    fun mount(mount: Boolean): PatchEditor = PatchEditor(bundles, unused, app, if (mount) InstallMethod.MOUNT else InstallMethod.INSTALL, choice, values)

    /** What a run takes: the choice, with the options the user set on the patches it takes up. */
    fun selection(): PatchSelection {
        val taken = planned?.patches.orEmpty().filter { it.run != PlannedRun.Off }.map { it.patch }.toSet()
        return known.copy(options = values.filterKeys { it in taken }.mapKeys { it.key.text })
    }

    private fun choose(choice: PatchSelection) = PatchEditor(bundles, unused, app, installMethod, choice, values)

    private fun chosen(row: PatchRow): Boolean =
        row.reference.text in choice.enable || choice.preset in row.planned.presets && row.reference.text !in choice.disable

    private fun needs(reference: PatchRef): Set<PatchRef> {
        val dependencies = patches.associate { it.reference to it.spec.dependencies }
        val found = linkedSetOf(reference)
        val pending = ArrayDeque(listOf(reference))
        while (pending.isNotEmpty()) dependencies[pending.removeFirst()].orEmpty().filter(found::add).forEach(pending::add)
        return found
    }

    private fun dependents(reference: PatchRef): Set<PatchRef> {
        val found = linkedSetOf(reference)
        val pending = ArrayDeque(listOf(reference))
        while (pending.isNotEmpty()) {
            val next = pending.removeFirst()
            enabled.filter { next in it.meta.spec.dependencies && found.add(it.reference) }.forEach { pending += it.reference }
        }
        return found
    }

    companion object {
        /** Patching [app] again: the choice its build was made with on today's patches, or the defaults for builds without one. */
        fun of(app: PatchedApp, bundles: Map<String, List<PatchMetadata>>, versionName: String?, allowIncompatible: Boolean): PatchEditor =
            from(bundles, app.packageName, versionName, allowIncompatible, app.installMethod, app.unusedBundles).let { editor ->
                app.choice?.let(editor::restore) ?: editor
            }

        fun from(
            bundles: Map<String, List<PatchMetadata>>,
            packageName: String,
            versionName: String?,
            allowIncompatible: Boolean,
            installMethod: InstallMethod = InstallMethod.INSTALL,
            unused: Set<String> = emptySet(),
        ): PatchEditor = PatchEditor(
            bundles = bundles,
            unused = unused,
            app = AppIdentity(packageName, versionName),
            installMethod = installMethod,
            choice = PatchSelection(preset = PatchPreset.RECOMMENDED, enable = emptyList(), disable = emptyList(), options = emptyMap(), ignoreVersions = allowIncompatible),
            values = emptyMap(),
        )
    }

    /**
     * [selection], an earlier choice, on these patches. Patches no longer available drop out of it, and so do values of
     * options their patches no longer declare, which have no field to change them. Patches it turns on that its preset
     * now takes drop out too, so the choice reads as that preset when nothing else changed.
     */
    private fun restore(selection: PatchSelection): PatchEditor {
        val declared = patches.associate { patch -> patch.reference to patch.spec.options.map { it.key }.toSet() }
        val values = selection.options.mapKeys { PatchRef.parse(it.key) }
            .mapValues { (reference, options) -> options.filterKeys { it in declared[reference].orEmpty() } }
            .filterValues { it.isNotEmpty() }
        val restored = PatchEditor(bundles, unused, app, installMethod, selection.copy(ignoreVersions = choice.ignoreVersions), values)
        val unknown = restored.planned?.unknown.orEmpty().toSet()
        val byPreset = restored.planned?.patches.orEmpty().filter { selection.preset in it.presets }.map { it.patch.text }.toSet()
        if (unknown.isEmpty() && selection.enable.none { it in byPreset }) return restored
        return PatchEditor(
            bundles,
            unused,
            app,
            installMethod,
            restored.choice.copy(enable = restored.choice.enable - unknown - byPreset, disable = restored.choice.disable - unknown),
            values,
        )
    }
}

private fun List<PatchRow>.tally(universal: Boolean): PatchCount = PatchCount(universal, count { it.enabled }, size)
