package app.reseam.manager.data

/** Ed25519 public key that signs the bundles the official API publishes. Rotating it means a new Manager release. */
const val OfficialSignerKey = "556c1b22f4e03398212ba10fe6a31db252df49f599b938d24a9f2b6baec41a1d"

/** Why a staged bundle waits for the user's decision instead of installing. */
sealed interface TrustPrompt {
    data object UnknownSigner : TrustPrompt

    data object NewApiSigner : TrustPrompt

    data class ChangedApiSigner(val previous: String) : TrustPrompt
}

class OfficialSignerMismatch : Exception(
    "The official API published a signing key this Reseam Manager does not know. Reinstall Reseam Manager from reseam.app before patching.",
)

/** The spec's trust table: the pinned key at the default API, trust on first use anywhere else. */
fun officialSignerPrompt(apiBaseUrl: String, key: String, installed: Bundle?): TrustPrompt? = when {
    apiBaseUrl == DefaultApiBaseUrl -> if (key == OfficialSignerKey) null else throw OfficialSignerMismatch()
    installed == null -> TrustPrompt.NewApiSigner
    installed.id == key -> null
    else -> TrustPrompt.ChangedApiSigner(installed.id)
}
