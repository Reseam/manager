package app.reseam.manager.sdk

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

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
    val applicationLabel: String? = null,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val bundleKind: String? = null,
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
    val problem: Problem? = null,
)

@Serializable
data class CompatiblePackage(
    val `package`: String,
    /** Empty means every version. */
    val versions: List<String> = emptyList(),
)

/** Which apps a patch declares itself for. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
sealed interface Compatibility {
    /** Declares no package, so it works with any app and stays opt-in. */
    @Serializable @SerialName("universal") data object Universal : Compatibility

    @Serializable @SerialName("packages") data class Packages(val packages: List<CompatiblePackage>) : Compatibility
}

@Serializable
data class PatchMetadata(
    val bundle: String,
    val id: String,
    val name: String = id,
    val hidden: Boolean = false,
    val description: String,
    val enabledByDefault: Boolean,
    val dependencies: List<String> = emptyList(),
    val compatibility: Compatibility,
    val options: List<OptionDeclaration> = emptyList(),
    val incompatibility: String? = null,
) {
    val universal: Boolean get() = compatibility is Compatibility.Universal

    /** The packages this patch declares, empty when it works with any app. */
    val declared: List<CompatiblePackage> get() = when (compatibility) {
        is Compatibility.Universal -> emptyList()
        is Compatibility.Packages -> compatibility.packages
    }

    fun supports(packageName: String): Boolean = universal || declared.any { it.`package` == packageName }
}
