package app.reseam.manager.data

import app.reseam.manager.platform.httpGetText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

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
