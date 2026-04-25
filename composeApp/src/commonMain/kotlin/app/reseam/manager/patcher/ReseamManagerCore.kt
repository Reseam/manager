package app.reseam.manager.patcher

class ReseamManagerCore(
    private val backend: ReseamBackend,
) {
    suspend fun inspect(input: ReseamInput): ReseamCallResult<InspectResponse> =
        backend.inspect(input.toInspectRequest())

    suspend fun patch(plan: PatchPlan, onEvent: (RunEvent) -> Unit = {}): ReseamCallResult<PatchOutcome> =
        backend.patch(plan.toPatchRequest(), onEvent)
}

data class ReseamInput(
    val apkPath: String? = null,
    val splitPaths: List<String> = emptyList(),
    val bundlePaths: List<String> = emptyList(),
    val trust: TrustConfig = TrustConfig(),
) {
    fun toInspectRequest(): InspectRequest =
        InspectRequest(
            apkPath = apkPath,
            splitPaths = splitPaths,
            bundlePaths = bundlePaths,
            includeBuiltinTrust = trust.includeBuiltinTrust,
            trustedPublicKeysHex = trust.trustedPublicKeysHex,
        )
}

data class PatchPlan(
    val apkPath: String,
    val splitPaths: List<String> = emptyList(),
    val bundlePaths: List<String> = emptyList(),
    val output: PatchOutput,
    val selection: PatchSelection = PatchSelection(),
    val trust: TrustConfig = TrustConfig(),
    val signing: SigningConfig = SigningConfig(),
    val dryRun: Boolean = false,
) {
    fun toPatchRequest(): PatchRequest =
        PatchRequest(
            apkPath = apkPath,
            splitPaths = splitPaths,
            bundlePaths = bundlePaths,
            output = output,
            selection = selection,
            includeBuiltinTrust = trust.includeBuiltinTrust,
            trustedPublicKeysHex = trust.trustedPublicKeysHex,
            keyPath = signing.keyPath,
            certPath = signing.certPath,
            dryRun = dryRun,
        )
}

data class TrustConfig(
    val includeBuiltinTrust: Boolean = true,
    val trustedPublicKeysHex: List<String> = emptyList(),
)

data class SigningConfig(
    val keyPath: String? = null,
    val certPath: String? = null,
)
