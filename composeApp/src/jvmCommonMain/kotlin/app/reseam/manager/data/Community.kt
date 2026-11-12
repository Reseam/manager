package app.reseam.manager.data

import app.reseam.manager.platform.httpGetText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/** A patch source the community organization lists, as the update server's `/v1/community` returns it. */
@Serializable
data class CommunityBundle(
    val slug: String,
    val name: String,
    val author: String,
    val description: String,
    val publicKey: String,
    /** Where the source lives: a mirror's original, or the repository itself. */
    val repository: String,
    val homepage: String? = null,
    val indexUrl: String,
    val version: String,
    val patches: Int,
    /** False once Reseam took the source off the list; it stays in the answer so people who added it can be told. */
    val listed: Boolean,
)

@Serializable
private data class CommunityDetail(val readme: String? = null)

sealed interface CommunityFeed {
    data object Loading : CommunityFeed
    data class Loaded(val bundles: List<CommunityBundle>) : CommunityFeed
    data class Failed(val error: Exception) : CommunityFeed
}

val CommunityFeed.bundles: List<CommunityBundle> get() = (this as? CommunityFeed.Loaded)?.bundles.orEmpty()

class CommunityRepository(private val settings: SettingsRepository) {
    private val feedState = MutableStateFlow<CommunityFeed>(CommunityFeed.Loading)
    val feed: StateFlow<CommunityFeed> = feedState.asStateFlow()

    private val serverUrl get() = settings.settings.value.serverUrl.trimEnd('/')

    suspend fun refresh() {
        if (feedState.value is CommunityFeed.Failed) feedState.value = CommunityFeed.Loading
        feedState.value = try {
            CommunityFeed.Loaded(ApiJson.decodeFromString(httpGetText("$serverUrl/v1/community")))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            (feedState.value as? CommunityFeed.Loaded) ?: CommunityFeed.Failed(error)
        }
    }

    /** Reads the list once for screens that only look things up in it. */
    suspend fun load() {
        if (feedState.value !is CommunityFeed.Loaded) refresh()
    }

    suspend fun readme(slug: String): String? =
        ApiJson.decodeFromString<CommunityDetail>(httpGetText("$serverUrl/v1/community/$slug")).readme?.let(::readmeMarkdown)
}

private val HtmlTag = Regex("<[^>]*>")
private val Spaces = Regex(" {2,}")
private val Image = Regex("""!\[([^\]]*)]\([^)]*\)""")
private val Alert = Regex("""^(\s*>\s*)\[!(\w+)]\s*$""", RegexOption.MULTILINE)
private val Fence = Regex("^\\s*(```|~~~)")

/**
 * READMEs are written for a repository page. Their HTML and images keep only their text (a badge inside a
 * link stays a link), code blocks stay as written, and GitHub's `[!NOTE]` alert markers become a bold label.
 */
private fun readmeMarkdown(text: String): String {
    var code = false
    return text.lines().joinToString("\n") { line ->
        if (Fence.containsMatchIn(line)) code = !code
        when {
            code || Fence.containsMatchIn(line) -> line
            HtmlTag.containsMatchIn(line) -> line.replace(HtmlTag, " ").replace(Spaces, " ").replace(Image, "$1").trim()
            else -> line.replace(Image, "$1").trimEnd()
        }
    }.replace(Alert) { match ->
        val kind = match.groupValues[2].lowercase().replaceFirstChar { it.uppercase() }
        "${match.groupValues[1]}**$kind**"
    }
}
