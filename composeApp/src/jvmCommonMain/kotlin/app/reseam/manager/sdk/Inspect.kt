package app.reseam.manager.sdk

import app.reseam.sdk.*

val PatchMetadata.bundle: String get() = spec.bundle
val PatchMetadata.reference: String get() = "$bundle/${spec.id}"
val PatchMetadata.id: String get() = spec.id
val PatchMetadata.name: String get() = spec.name
val PatchMetadata.hidden: Boolean get() = spec.hidden
val PatchMetadata.description: String get() = spec.description
val PatchMetadata.enabledByDefault: Boolean get() = spec.enabledByDefault
val PatchMetadata.dependencies: List<String> get() = spec.dependencies
val PatchMetadata.compatibility: Compatibility get() = spec.compatibility
val PatchMetadata.options: List<OptionDeclaration> get() = spec.options
val PatchMetadata.universal: Boolean get() = compatibility is Compatibility.Universal
val PatchMetadata.declared: List<CompatiblePackage> get() = when (val value = compatibility) {
    is Compatibility.Universal -> emptyList()
    is Compatibility.Packages -> value.packages
}
fun PatchMetadata.supports(packageName: String): Boolean = universal || declared.any { it.`package` == packageName }
