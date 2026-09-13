package app.reseam.manager.data

import app.reseam.manager.platform.httpGetText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

@Serializable
data class OfficialRelease(val bundle: OfficialBundleInfo, val release: OfficialReleaseInfo)

@Serializable
data class OfficialBundleInfo(val name: String)

@Serializable
data class OfficialReleaseInfo(val version: String, val downloadUrl: String)

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
suspend fun fetchManagerRelease(apiBaseUrl: String): OfficialReleaseInfo =
    IndexJson.decodeFromString<OfficialRelease>(httpGetText(apiBaseUrl.trimEnd('/') + "/manager")).release

/** Semver order on `major.minor.patch`; anything unparseable never counts as newer. */
fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(version: String): List<Int>? = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }
    val next = parts(candidate) ?: return false
    val now = parts(current) ?: return false
    return next.zip(now).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a > b } ?: (next.size > now.size)
}

suspend fun fetchOfficialKey(apiBaseUrl: String): String =
    IndexJson.decodeFromString<OfficialKey>(httpGetText(apiBaseUrl.trimEnd('/') + "/patches/keys")).publicKey
