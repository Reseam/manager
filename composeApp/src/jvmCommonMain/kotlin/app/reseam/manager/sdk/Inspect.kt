package app.reseam.manager.sdk

import app.reseam.sdk.Compatibility
import app.reseam.sdk.CompatiblePackage
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.PatchRef

val PatchMetadata.reference: PatchRef get() = PatchRef(spec.bundle, spec.id)

/** The `<bundle>/<id>` form selections are sent and saved in. */
val PatchRef.text: String get() = "$bundle/$id"
fun PatchRef.Companion.parse(text: String): PatchRef = PatchRef(text.substringBefore('/'), text.substringAfter('/'))
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
