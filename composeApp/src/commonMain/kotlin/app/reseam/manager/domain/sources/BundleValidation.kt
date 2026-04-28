package app.reseam.manager.domain.sources

import app.reseam.manager.patcher.InspectRequest
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.ui.model.toSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun ReseamBackend.validateBundle(
    path: String,
    source: String,
    autoTrust: Boolean = false,
): BundleImportResult = withContext(Dispatchers.Default) {
    val firstPass = runInspect(path, trustedKeys = emptyList())

    val metadata = requireNotNull(firstPass.bundles.singleOrNull()) { "Bundle metadata missing after validation" }
    check(metadata.name.isNotBlank()) { "Bundle name is missing" }
    check(metadata.signerPublicKeyHex.isNotBlank()) { "Bundle signer is missing" }
    check(metadata.signerFingerprint.isNotBlank()) { "Bundle signer fingerprint is missing" }

    val response = if (firstPass.requiresTrust && autoTrust) {
        runInspect(path, trustedKeys = listOf(metadata.signerPublicKeyHex))
    } else {
        firstPass
    }

    val patches = response.patches.filter { it.sourceBundle == metadata.name }
    BundleImportResult(
        summary = metadata.toSummary(path = path, source = source, patchCount = patches.size),
        patches = patches,
    )
}

private suspend fun ReseamBackend.runInspect(path: String, trustedKeys: List<String>) =
    when (val result = inspect(
        InspectRequest(
            bundlePaths = listOf(path),
            includeBuiltinTrust = true,
            trustedPublicKeysHex = trustedKeys,
        )
    )) {
        is ReseamCallResult.Success -> result.value
        is ReseamCallResult.Failure -> throw IllegalStateException(result.message)
    }
