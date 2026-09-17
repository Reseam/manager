package app.reseam.manager.data

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
enum class SavedApkOrigin { Download, File }

@Serializable
data class SourceBuild(val source: String, val id: String)

@Serializable
data class SavedApk(
    val id: String,
    val packageName: String?,
    val name: String,
    val versionName: String?,
    val path: String,
    val iconPath: String? = null,
    val sizeBytes: Long,
    val origin: SavedApkOrigin,
    val sourceBuild: SourceBuild? = null,
    val savedAtEpochMs: Long,
)

@Serializable
data class SavedApkLibrary(val apks: List<SavedApk> = emptyList())

/** Kept with the app's data rather than its cache, so the system never removes an APK a re-patch still needs. */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
class SavedApkRepository(
    private val store: JsonStore<SavedApkLibrary>,
    private val directory: PlatformFile,
    private val identities: AppIdentityReader,
) {
    private val staging = directory / "staging"
    private val icons = directory / "icons"

    val apks: Flow<List<SavedApk>> = store.state.map { it.apks }

    fun downloaded(build: SourceBuild): SavedApk? = store.state.value.apks.firstOrNull { it.sourceBuild == build }

    /** Staged under the real extension because the engine tells containers apart by it. The same package, version, and size replaces an older copy. */
    suspend fun save(extension: String, origin: SavedApkOrigin, sourceBuild: SourceBuild? = null, write: suspend (PlatformFile) -> Unit): SavedApk {
        val id = Uuid.random().toString()
        val staged = staging / "$id.$extension"
        withContext(Dispatchers.IO) { staging.createDirectories() }
        try {
            write(staged)
            val identity = identities.read(staged.absolutePath())
            val file = directory / "$id.$extension"
            val icon = identity.iconPath?.let { icons / id }
            withContext(Dispatchers.IO) {
                staged.atomicMove(file)
                if (icon != null) {
                    icons.createDirectories()
                    PlatformFile(identity.iconPath).copyTo(icon)
                }
            }
            val saved = SavedApk(
                id = id,
                packageName = identity.packageName,
                name = identity.name,
                versionName = identity.versionName,
                path = file.absolutePath(),
                iconPath = icon?.absolutePath(),
                sizeBytes = file.size(),
                origin = origin,
                sourceBuild = sourceBuild,
                savedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            )
            val replaced = store.state.value.apks.filter {
                it.packageName == saved.packageName && it.versionName == saved.versionName && it.sizeBytes == saved.sizeBytes
            }
            store.update { library -> library.copy(apks = library.apks.filterNot { it in replaced } + saved) }
            replaced.forEach { deleteFiles(it) }
            return saved
        } catch (error: Exception) {
            withContext(Dispatchers.IO) { staged.delete(mustExist = false) }
            throw error
        }
    }

    suspend fun remove(id: String) {
        val apk = store.state.value.apks.firstOrNull { it.id == id } ?: return
        store.update { library -> library.copy(apks = library.apks - apk) }
        deleteFiles(apk)
    }

    suspend fun clearStaging() = withContext(Dispatchers.IO) {
        if (staging.exists()) staging.list().forEach { it.delete(mustExist = false) }
    }

    private suspend fun deleteFiles(apk: SavedApk) = withContext(Dispatchers.IO) {
        PlatformFile(apk.path).delete(mustExist = false)
        apk.iconPath?.let { PlatformFile(it).delete(mustExist = false) }
    }
}
