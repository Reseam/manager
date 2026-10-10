package app.reseam.manager.data

import app.reseam.sdk.PatchMetadata
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseNote(val version: String, val createdAt: String, val description: String)

@Serializable
data class PatchNote(val name: String, val description: String)

@Serializable
data class WhatsNew(
    val version: String,
    val notes: List<ReleaseNote>,
    val added: List<PatchNote>,
    val removed: List<PatchNote>,
    val changedOptions: Int,
) {
    val isEmpty: Boolean get() = notes.none { it.description.isNotBlank() } && added.isEmpty() && removed.isEmpty() && changedOptions == 0

    fun then(next: WhatsNew): WhatsNew = WhatsNew(
        version = next.version,
        notes = next.notes + notes,
        added = (added.filterNot { patch -> next.removed.any { it.name == patch.name } } + next.added).distinctBy { it.name },
        removed = (removed.filterNot { patch -> next.added.any { it.name == patch.name } } + next.removed).distinctBy { it.name },
        changedOptions = changedOptions + next.changedOptions,
    )
}

data class BundleOffer(
    val bundleId: String,
    val name: String,
    val release: ReleaseInfo,
    val key: String,
    val source: UpdateSource,
    val prompt: TrustPrompt?,
    val notes: List<ReleaseNote>,
)

fun ReleaseInfo.note(): ReleaseNote = ReleaseNote(version, createdAt, description)

fun List<ReleaseInfo>.notesSince(installed: String?): List<ReleaseNote> =
    if (installed == null) emptyList() else filter { !it.prerelease && isNewerVersion(it.version, installed) }.map { it.note() }

fun whatsNew(version: String, notes: List<ReleaseNote>, before: List<PatchMetadata>, after: List<PatchMetadata>): WhatsNew {
    val old = before.filterNot { it.spec.hidden }.associateBy { it.spec.id }
    val new = after.filterNot { it.spec.hidden }.associateBy { it.spec.id }
    return WhatsNew(
        version = version,
        notes = notes,
        added = new.filterKeys { it !in old }.values.map { PatchNote(it.spec.name, it.spec.description) },
        removed = old.filterKeys { it !in new }.values.map { PatchNote(it.spec.name, it.spec.description) },
        changedOptions = new.count { (id, patch) -> old[id]?.let { it.spec.options != patch.spec.options } == true },
    )
}
