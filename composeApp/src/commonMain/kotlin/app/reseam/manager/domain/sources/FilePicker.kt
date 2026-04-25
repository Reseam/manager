package app.reseam.manager.domain.sources

data class PickedFile(
    val displayName: String,
    val path: String,
)

interface FilePicker {
    suspend fun pickApk(): PickedFile?
    suspend fun pickPatchBundle(): PickedFile?
}

object EmptyFilePicker : FilePicker {
    override suspend fun pickApk(): PickedFile? = null
    override suspend fun pickPatchBundle(): PickedFile? = null
}
