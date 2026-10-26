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

fun List<ReleaseInfo>.notesSince(installed: String?, prereleases: Boolean): List<ReleaseNote> =
    if (installed == null) emptyList() else filter { (prereleases || !it.prerelease) && isNewerVersion(it.version, installed) }.map { it.note() }

private val ConventionalItem = Regex("""^\s*[-*+]\s+(\w+)(?:\(([^)]+)\))?!?:\s+(.+)$""")
private val ScopedItem = Regex("""^\s*[-*+]\s+\*\*([^*]+):\*\*\s+(.+)$""")
private val SectionTitles = linkedMapOf(
    "feat" to "New",
    "fix" to "Fixed",
    "perf" to "Faster",
)
private val AppNames = mapOf("youtube" to "YouTube", "youtube-music" to "YouTube Music", "tiktok" to "TikTok")

/**
 * The description as markdown without its own version heading. Plain `- type(scope): text` commit lines are grouped
 * into New, Fixed and Faster by app, the same way reseam.app shows them; build, style and other internal commits are
 * left out. Every change is capitalized, so both styles of bundle read the same.
 */
fun ReleaseNote.markdown(): String {
    val body = description.withoutVersionHeading(version)
    val lines = body.lines().map { line -> ScopedItem.matchEntire(line)?.destructured?.let { (scope, text) -> change(scope, text) } ?: line }
        .map { it to ConventionalItem.matchEntire(it)?.destructured }
    val items = lines.mapNotNull { it.second }.filter { (type) -> type.lowercase() in SectionTitles }
    val prose = lines.filter { it.second == null }.joinToString("\n") { it.first }.trim()
    if (items.isEmpty()) return prose
    val sections = items.groupBy { (type) -> type.lowercase() }.toList()
        .sortedBy { (type) -> SectionTitles.keys.indexOf(type) }
        .joinToString("\n\n") { (type, entries) ->
            "### ${SectionTitles.getValue(type)}\n\n" + entries.joinToString("\n") { (_, scope, text) -> change(scope, text) }
        }
    return listOf(prose, sections).filter { it.isNotEmpty() }.joinToString("\n\n")
}

private fun change(scope: String, text: String): String {
    val title = AppNames[scope] ?: scope.split('-', '_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    val sentence = text.replaceFirstChar(Char::uppercase)
    return if (scope.isEmpty()) "- $sentence" else "- **$title:** $sentence"
}

private fun String.withoutVersionHeading(version: String): String {
    val first = trimStart().substringBefore('\n')
    return if (first.startsWith("#") && version in first) trimStart().substringAfter('\n', "").trim() else trim()
}
