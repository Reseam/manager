package app.reseam.manager.data

import app.reseam.manager.platform.httpGetText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.HttpUrl

private const val IndexFileName = "patches.json"

@Serializable
data class ReleaseInfo(
    val version: String,
    val downloadUrl: String,
    val prerelease: Boolean = false,
    val description: String = "",
    val createdAt: String = "",
)

@Serializable
data class BundleIndex(val bundle: IndexPublisher, val releases: List<ReleaseInfo>) {
    /** The newest release on the chosen channel; `reseam release` writes an index newest first. */
    fun latest(prereleases: Boolean): ReleaseInfo = releases.firstOrNull { prereleases || !it.prerelease } ?: throw Failure.NoStableRelease(bundle.name)
}

@Serializable
data class IndexPublisher(val name: String, val publicKey: String)

@Serializable
private data class ManagerIndex(val releases: List<ReleaseInfo>)

@OptIn(ExperimentalSerializationApi::class)
internal val ApiJson = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
}

fun officialIndexUrl(serverUrl: String): String = "${serverUrl.trimEnd('/')}/$IndexFileName"

suspend fun fetchManagerReleases(serverUrl: String): List<ReleaseInfo> =
    ApiJson.decodeFromString<ManagerIndex>(httpGetText("${serverUrl.trimEnd('/')}/manager.json")).releases

suspend fun fetchBundleIndex(url: String): BundleIndex = parseBundleIndex(url, httpGetText(url))

/** Where GitHub, Forgejo and Gitea, then GitLab serve a file from a repository's latest release, tried for a pasted repository page. */
fun latestIndexLinks(page: HttpUrl): List<String> {
    val segments = page.pathSegments.filter { it.isNotEmpty() }
    if (segments.size < 2) return emptyList()
    fun at(path: List<String>, vararg rest: String) = page.newBuilder().encodedPath("/").query(null).fragment(null).apply { (path + rest).forEach(::addPathSegment) }.build().toString()
    val repository = segments.take(2)
    val project = segments.takeWhile { it != "-" }
    return listOf(
        at(repository, "releases", "latest", "download", IndexFileName),
        at(repository, "releases", "download", "latest", IndexFileName),
        at(project, "-", "releases", "permalink", "latest", "downloads", IndexFileName),
    ).distinct()
}

fun parseBundleIndex(url: String, json: String): BundleIndex = try {
    ApiJson.decodeFromString(json)
} catch (_: SerializationException) {
    throw Failure.NotABundle(url)
}

/** Semantic version order: numbers first, then a release beats its prereleases, which compare part by part. */
fun isNewerVersion(candidate: String, current: String): Boolean = compareVersions(candidate, current)?.let { it > 0 } ?: false

private fun compareVersions(a: String, b: String): Int? {
    fun numbers(version: String): List<Int>? = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }
    val left = numbers(a) ?: return null
    val right = numbers(b) ?: return null
    left.zip(right).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> return x.compareTo(y) }
    if (left.size != right.size) return left.size.compareTo(right.size)
    val leftTag = a.substringAfter('-', "")
    val rightTag = b.substringAfter('-', "")
    if (leftTag.isEmpty() || rightTag.isEmpty()) return leftTag.isEmpty().compareTo(rightTag.isEmpty())
    leftTag.split('.').zip(rightTag.split('.')).forEach { (x, y) ->
        val order = when {
            x.toIntOrNull() != null && y.toIntOrNull() != null -> x.toInt().compareTo(y.toInt())
            else -> x.compareTo(y)
        }
        if (order != 0) return order
    }
    return leftTag.split('.').size.compareTo(rightTag.split('.').size)
}
