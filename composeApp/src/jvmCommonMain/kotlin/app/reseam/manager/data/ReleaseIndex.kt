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
data class OfficialRelease(val bundle: OfficialBundleInfo, val release: ReleaseInfo)

@Serializable
data class OfficialBundleInfo(val name: String)

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
    val latest: ReleaseInfo get() = releases.firstOrNull { !it.prerelease } ?: throw Failure.NoStableRelease(bundle.name)
}

@Serializable
data class IndexPublisher(val name: String, val publicKey: String)

@Serializable
private data class OfficialKey(val publicKey: String)

@OptIn(ExperimentalSerializationApi::class)
internal val ApiJson = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
}

@Serializable
private data class VersionResponse(val version: String)

@Serializable
private data class ReleaseHistory(val releases: List<ReleaseInfo>)

suspend fun fetchOfficialVersion(apiBaseUrl: String): String =
    ApiJson.decodeFromString<VersionResponse>(httpGetText(apiBaseUrl.trimEnd('/') + "/patches/version")).version

suspend fun fetchOfficialHistory(apiBaseUrl: String): List<ReleaseInfo> =
    ApiJson.decodeFromString<ReleaseHistory>(httpGetText(apiBaseUrl.trimEnd('/') + "/patches/history")).releases

suspend fun fetchManagerRelease(apiBaseUrl: String): ReleaseInfo =
    ApiJson.decodeFromString<OfficialRelease>(httpGetText(apiBaseUrl.trimEnd('/') + "/manager")).release

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

fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(version: String): List<Int>? = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }
    val next = parts(candidate) ?: return false
    val now = parts(current) ?: return false
    return next.zip(now).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a > b } ?: (next.size > now.size)
}

suspend fun fetchOfficialKey(apiBaseUrl: String): String =
    ApiJson.decodeFromString<OfficialKey>(httpGetText(apiBaseUrl.trimEnd('/') + "/patches/keys")).publicKey
