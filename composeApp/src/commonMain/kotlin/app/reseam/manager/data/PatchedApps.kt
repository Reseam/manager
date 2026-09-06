package app.reseam.manager.data

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class AppliedPatch(val id: String, val bundle: String)

@Serializable
data class PatchedApp(
    val packageName: String,
    val name: String,
    val versionName: String?,
    val apkPath: String,
    val sourceApkPath: String? = null,
    val sourceSplitPaths: List<String> = emptyList(),
    val patches: List<AppliedPatch>,
    val patchedAtEpochMs: Long,
)

@Serializable
data class PatchedAppLibrary(val apps: List<PatchedApp> = emptyList())

class PatchedAppRepository(
    private val store: JsonStore<PatchedAppLibrary>,
    private val directory: PlatformFile,
) {
    val apps: Flow<List<PatchedApp>> = store.state.map { it.apps }

    fun find(packageName: String): Flow<PatchedApp?> = apps.map { list -> list.firstOrNull { it.packageName == packageName } }

    suspend fun outputFile(packageName: String): PlatformFile = withContext(Dispatchers.IO) {
        directory.createDirectories()
        directory / "$packageName.reseamed.apk"
    }

    suspend fun outputDirectory(packageName: String): PlatformFile = withContext(Dispatchers.IO) {
        directory.createDirectories()
        directory / "$packageName.reseamed"
    }

    suspend fun save(app: PatchedApp) {
        store.update { it.copy(apps = it.apps.filterNot { existing -> existing.packageName == app.packageName } + app) }
    }

    suspend fun remove(packageName: String) {
        val app = store.state.value.apps.firstOrNull { it.packageName == packageName } ?: return
        store.update { it.copy(apps = it.apps - app) }
        withContext(Dispatchers.IO) { PlatformFile(app.apkPath).deleteRecursively() }
    }
}

private suspend fun PlatformFile.deleteRecursively() {
    if (isDirectory()) list().forEach { it.deleteRecursively() }
    delete(mustExist = false)
}
