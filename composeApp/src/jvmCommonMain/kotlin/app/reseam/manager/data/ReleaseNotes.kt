package app.reseam.manager.data

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseNote(val version: String, val createdAt: String, val description: String)

@Serializable
data class WhatsNew(val version: String, val notes: List<ReleaseNote>) {
    val isEmpty: Boolean get() = notes.none { it.description.isNotBlank() }

    fun then(next: WhatsNew): WhatsNew = WhatsNew(next.version, next.notes + notes)
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
