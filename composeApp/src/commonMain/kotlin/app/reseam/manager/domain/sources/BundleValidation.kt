package app.reseam.manager.domain.sources

import app.reseam.manager.patcher.InspectRequest
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.toSummary

suspend fun ReseamBackend.validateBundle(path: String, source: String): BundleSummary {
    val result = inspect(
        InspectRequest(
            bundlePaths = listOf(path),
            includeBuiltinTrust = true,
            trustedPublicKeysHex = emptyList(),
        ),
    )

    val response = when (result) {
        is ReseamCallResult.Success -> result.value
        is ReseamCallResult.Failure -> error(result.message)
    }

    val metadata = response.bundles.singleOrNull()
        ?: error("Bundle metadata missing after validation")

    require(metadata.name.isNotBlank()) { "Bundle name is missing" }
    require(metadata.signerPublicKeyHex.isNotBlank()) { "Bundle signer is missing" }
    require(metadata.signerFingerprint.isNotBlank()) { "Bundle signer fingerprint is missing" }

    return metadata.toSummary(
        path = path,
        source = source,
        patchCount = response.patches.count { it.sourceBundle == metadata.name },
    )
}
