package app.reseam.manager.domain.sources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val OfficialPatchesPublicKeyHex = "556c1b22f4e03398212ba10fe6a31db252df49f599b938d24a9f2b6baec41a1d"
const val OfficialBundleId = "reseam-patches"

fun officialPatchesIndexUrl(apiBaseUrl: String): String =
    apiBaseUrl.trimEnd('/') + "/patches"

@Serializable
data class OfficialPatchesIndex(
    val bundle: OfficialBundleInfo,
    val release: OfficialBundleRelease? = null,
    val releases: List<OfficialBundleRelease> = emptyList(),
) {
    fun latestStableRelease(): OfficialBundleRelease? =
        release?.takeUnless { it.prerelease }
            ?: releases.firstOrNull { !it.prerelease }
            ?: release
            ?: releases.firstOrNull()
}

@Serializable
data class OfficialBundleInfo(
    val name: String,
    @SerialName("public_key")
    val publicKey: String,
)

@Serializable
data class OfficialBundleRelease(
    val version: String,
    @SerialName("download_url")
    val downloadUrl: String,
    val prerelease: Boolean = false,
)

fun BundleImportResult.markOfficial(
    release: OfficialBundleRelease,
    expectedPublicKeyHex: String,
): BundleImportResult {
    val expected = expectedPublicKeyHex.lowercase()
    check(summary.signerPublicKeyHex?.lowercase() == expected) { "Downloaded official bundle signer mismatch" }
    check(summary.trusted) { "Downloaded official bundle is not trusted" }
    check(summary.patchCount > 0) { "Downloaded official bundle contains no patches" }
    return copy(
        summary = summary.copy(
            id = OfficialBundleId,
            official = true,
            trusted = true,
            version = release.version,
            source = release.downloadUrl,
        ),
    )
}
