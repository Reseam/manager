package app.reseam.manager.sdk

import kotlinx.serialization.Serializable

@Serializable
data class Trust(val keys: List<String> = emptyList())

@Serializable
data class InspectRequest(
    val apkPath: String? = null,
    val splitPaths: List<String> = emptyList(),
    val bundlePaths: List<String> = emptyList(),
    val trust: Trust = Trust(),
)

@Serializable
data class InspectResponse(
    val apk: ApkMetadata? = null,
    val bundles: List<BundleMetadata> = emptyList(),
    val patches: List<PatchMetadata> = emptyList(),
)

@Serializable
data class ApkMetadata(
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val dexFiles: Int,
    val componentCount: Int,
    val splitNames: List<String>,
    val classCount: Int,
    val methodCount: Int,
)

@Serializable
data class BundleMetadata(
    val fileName: String,
    val name: String,
    val author: String,
    val description: String,
    val files: List<String>,
    val publicKey: String,
    val trusted: Boolean,
)

@Serializable
data class Compatibility(
    val `package`: String,
    val versions: List<String> = emptyList(),
)

@Serializable
data class PatchMetadata(
    val bundle: String,
    val id: String,
    val description: String,
    val enabledByDefault: Boolean,
    val dependencies: List<String> = emptyList(),
    val compatibility: List<Compatibility> = emptyList(),
    val options: List<OptionDeclaration> = emptyList(),
    val incompatibility: String? = null,
) {
    fun supports(packageName: String): Boolean =
        compatibility.isEmpty() || compatibility.any { it.`package` == packageName }
}
