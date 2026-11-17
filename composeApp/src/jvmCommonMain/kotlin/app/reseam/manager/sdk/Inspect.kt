package app.reseam.manager.sdk

import androidx.compose.ui.text.intl.LocaleList
import app.reseam.sdk.Compatibility
import app.reseam.sdk.CompatiblePackage
import app.reseam.sdk.OptionText
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.PatchRef
import app.reseam.sdk.PatchText
import app.reseam.sdk.patchText

val PatchMetadata.reference: PatchRef get() = PatchRef(spec.bundle, spec.id)

/** The `<bundle>/<id>` form selections are sent and saved in. */
val PatchRef.text: String get() = "$bundle/$id"
fun PatchRef.Companion.parse(text: String): PatchRef = PatchRef(text.substringBefore('/'), text.substringAfter('/'))
val PatchMetadata.universal: Boolean get() = spec.compatibility is Compatibility.Universal
val PatchMetadata.declared: List<CompatiblePackage> get() = when (val value = spec.compatibility) {
    is Compatibility.Universal -> emptyList()
    is Compatibility.Packages -> value.packages
}
/** What the user sees of the patch, in the languages they read, most preferred first. */
fun PatchMetadata.text(): PatchText = patchText(spec, LocaleList.current.localeList.map { it.toLanguageTag() })

/** A patch with its text resolved once, for lists the user searches. */
data class LocalizedPatch(val meta: PatchMetadata, val text: PatchText)

fun PatchText.option(key: String): OptionText = options.first { it.key == key }

fun PatchText.matches(query: String): Boolean {
    val needle = query.trim().lowercase()
    return needle.isEmpty() || needle in name.lowercase() || needle in description.lowercase()
}
