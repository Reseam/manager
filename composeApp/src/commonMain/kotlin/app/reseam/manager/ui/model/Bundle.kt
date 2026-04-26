package app.reseam.manager.ui.model

import app.reseam.manager.patcher.BundleMetadata
import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.patcher.TrustConfig
import app.reseam.manager.patcher.TrustStatus
import kotlinx.serialization.Serializable

enum class BundleSourceKind {
    Official,
    Url,
    File,
}

val BundleSummary.kind: BundleSourceKind
    get() = when {
        official -> BundleSourceKind.Official
        source?.startsWith("http://") == true || source?.startsWith("https://") == true -> BundleSourceKind.Url
        else -> BundleSourceKind.File
    }

data class BundleDetailContent(
    val bundle: BundleSummary,
    val patches: List<PatchMetadata>,
)

data class BundlesState(
    val installed: List<BundleSummary> = emptyList(),
    val pendingTrust: PendingBundleTrust? = null,
    val importing: Boolean = false,
    val detail: BundleDetailContent? = null,
) {
    val trustedPublicKeysHex: List<String>
        get() = installed
            .filter { it.trusted && !it.official && it.signerPublicKeyHex != null }
            .mapNotNull { it.signerPublicKeyHex }

    fun toTrustConfig(): TrustConfig =
        TrustConfig(
            includeBuiltinTrust = installed.any { it.official && it.trusted },
            trustedPublicKeysHex = trustedPublicKeysHex,
        )
}

@Serializable
data class BundleSummary(
    val id: String,
    val name: String,
    val description: String,
    val source: String? = null,
    val official: Boolean = false,
    val trusted: Boolean = false,
    val patchCount: Int = 0,
    val version: String? = null,
    val updatedLabel: String? = null,
    val signerPublicKeyHex: String? = null,
    val signerFingerprint: String? = null,
    val path: String? = null,
)

data class PendingBundleTrust(
    val bundle: BundleSummary,
    val warning: String = "Only trust bundles from sources you know. Trusted bundles can modify apps.",
)

fun BundleMetadata.toSummary(
    path: String? = null,
    source: String? = null,
    patchCount: Int = 0,
): BundleSummary =
    BundleSummary(
        id = signerPublicKeyHex.ifBlank { fileName },
        name = name,
        description = description,
        source = source,
        trusted = trustStatus == TrustStatus.Trusted,
        patchCount = patchCount,
        signerPublicKeyHex = signerPublicKeyHex,
        signerFingerprint = signerFingerprint,
        path = path,
    )
