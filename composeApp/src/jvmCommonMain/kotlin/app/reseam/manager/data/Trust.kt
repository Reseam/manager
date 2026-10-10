package app.reseam.manager.data

const val OfficialSignerKey = "556c1b22f4e03398212ba10fe6a31db252df49f599b938d24a9f2b6baec41a1d"

sealed interface TrustPrompt {
    data object UnknownSigner : TrustPrompt

    data object NewApiSigner : TrustPrompt

    data class ChangedSigner(val previous: String) : TrustPrompt
}

fun officialSignerPrompt(apiBaseUrl: String, key: String, installed: Bundle?): TrustPrompt? = when {
    apiBaseUrl == DefaultApiBaseUrl -> if (key == OfficialSignerKey) null else throw Failure.OfficialSignerChanged()
    installed == null -> TrustPrompt.NewApiSigner
    installed.id == key -> null
    else -> TrustPrompt.ChangedSigner(installed.id)
}
