package app.reseam.manager.data

import app.reseam.manager.platform.httpGetText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@Serializable
data class OfficialRelease(val bundle: OfficialBundleInfo, val release: ReleaseInfo)

@Serializable
data class OfficialBundleInfo(val name: String)

@Serializable
data class ReleaseInfo(val version: String, val downloadUrl: String, val prerelease: Boolean = false)

@Serializable
data class BundleIndex(val bundle: IndexPublisher, val releases: List<ReleaseInfo>) {
    val latest: ReleaseInfo get() = releases.firstOrNull { !it.prerelease } ?: error("${bundle.name} has no stable release")
}

@Serializable
data class IndexPublisher(val name: String, val publicKey: String)

@Serializable
private data class OfficialKey(val publicKey: String)

@OptIn(ExperimentalSerializationApi::class)
private val IndexJson = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
}

suspend fun fetchOfficialRelease(apiBaseUrl: String): OfficialRelease =
    IndexJson.decodeFromString(httpGetText(apiBaseUrl.trimEnd('/') + "/patches"))

/** `manager.json` has the same shape as `patches.json`; only the latest stable release matters here. */
suspend fun fetchManagerRelease(apiBaseUrl: String): ReleaseInfo =
    IndexJson.decodeFromString<OfficialRelease>(httpGetText(apiBaseUrl.trimEnd('/') + "/manager")).release

suspend fun fetchBundleIndex(url: String): BundleIndex = parseBundleIndex(url, httpGetText(url))

fun parseBundleIndex(url: String, json: String): BundleIndex = try {
    IndexJson.decodeFromString(json)
} catch (_: SerializationException) {
    throw IllegalArgumentException("$url is neither a bundle nor a bundle's patches.json")
}

/** Semver order on `major.minor.patch`; anything unparseable never counts as newer. */
fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(version: String): List<Int>? = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }
    val next = parts(candidate) ?: return false
    val now = parts(current) ?: return false
    return next.zip(now).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a > b } ?: (next.size > now.size)
}

suspend fun fetchOfficialKey(apiBaseUrl: String): String =
    IndexJson.decodeFromString<OfficialKey>(httpGetText(apiBaseUrl.trimEnd('/') + "/patches/keys")).publicKey
