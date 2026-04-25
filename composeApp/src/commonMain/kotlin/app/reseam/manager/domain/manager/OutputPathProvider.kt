package app.reseam.manager.domain.manager

import app.reseam.manager.ui.model.PatchInput

interface OutputPathProvider {
    fun outputFor(input: PatchInput): String
}

class DefaultOutputPathProvider(
    private val directory: String,
) : OutputPathProvider {
    override fun outputFor(input: PatchInput): String {
        val safeName = input.displayName
            .lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .ifBlank { "patched" }
        return "$directory/$safeName.reseamed.apk"
    }
}
