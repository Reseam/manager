package app.reseam.manager.data

import app.reseam.manager.platform.httpGetText
import app.reseam.sdk.PublishedRelease
import app.reseam.sdk.ReleaseChannel
import app.reseam.sdk.ReleaseIndex
import app.reseam.sdk.SdkError
import app.reseam.sdk.decodeReleaseIndex
import app.reseam.sdk.latestRelease
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.HttpUrl

private const val IndexFileName = "patches.json"

@OptIn(ExperimentalSerializationApi::class)
internal val ApiJson = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
}

/** The bundle signer a patches index names, which [parseBundleIndex] requires. */
val ReleaseIndex.signer: String get() = checkNotNull(bundle.publicKey) { "parseBundleIndex accepts only indexes that name their signer" }

/** The newest release on the chosen channel. */
fun ReleaseIndex.latest(prereleases: Boolean): PublishedRelease =
    latestRelease(this, if (prereleases) ReleaseChannel.PRERELEASE else ReleaseChannel.STABLE) ?: throw Failure.NoStableRelease(bundle.name)

fun officialIndexUrl(serverUrl: String): String = "${serverUrl.trimEnd('/')}/$IndexFileName"

suspend fun fetchManagerReleases(serverUrl: String): List<PublishedRelease> =
    decodeReleaseIndex(httpGetText("${serverUrl.trimEnd('/')}/manager.json")).releases

suspend fun fetchBundleIndex(url: String): ReleaseIndex = parseBundleIndex(url, httpGetText(url))

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

fun parseBundleIndex(url: String, json: String): ReleaseIndex {
    val index = try {
        decodeReleaseIndex(json)
    } catch (_: SdkError) {
        throw Failure.NotABundle(url)
    }
    if (index.bundle.publicKey == null) throw Failure.NotABundle(url)
    return index
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
