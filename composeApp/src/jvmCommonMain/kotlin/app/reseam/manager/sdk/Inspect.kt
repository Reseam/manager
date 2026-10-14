package app.reseam.manager.sdk

import app.reseam.sdk.Compatibility
import app.reseam.sdk.CompatiblePackage
import app.reseam.sdk.PatchMetadata

val PatchMetadata.reference: String get() = "${spec.bundle}/${spec.id}"
val PatchMetadata.universal: Boolean get() = spec.compatibility is Compatibility.Universal
val PatchMetadata.declared: List<CompatiblePackage> get() = when (val value = spec.compatibility) {
    is Compatibility.Universal -> emptyList()
    is Compatibility.Packages -> value.packages
}
fun PatchMetadata.supports(packageName: String): Boolean = universal || declared.any { it.`package` == packageName }
fun PatchMetadata.matches(query: String): Boolean {
    val needle = query.trim().lowercase()
    return needle.isEmpty() || needle in spec.name.lowercase() || needle in spec.description.lowercase()
}
