package app.reseam.manager.data

import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class AppliedPatch(val id: String, val bundle: String, val name: String = id)

@Serializable
data class PatchedApp(
    val packageName: String,
    val name: String,
    val versionName: String?,
    val apkPath: String,
    val sourceApkPath: String? = null,
    val iconPath: String? = null,
    val sourceSplitPaths: List<String> = emptyList(),
    val patches: List<AppliedPatch>,
    val patchedAtEpochMs: Long,
    val installMethod: InstallMethod = InstallMethod.INSTALL,
    val installedAs: String? = null,
)

@Serializable
data class PatchedAppLibrary(val apps: List<PatchedApp> = emptyList())

class PatchedAppRepository(
    private val store: JsonStore<PatchedAppLibrary>,
    private val directory: PlatformFile,
) {
    private val staging = directory / "staging"

    val apps: Flow<List<PatchedApp>> = store.state.map { it.apps }

    fun find(packageName: String): Flow<PatchedApp?> = apps.map { list -> list.firstOrNull { it.packageName == packageName } }

    /** Runs write here, so a failed run leaves the previous output intact. */
    suspend fun stage(packageName: String): PlatformFile = withContext(Dispatchers.IO) {
        discardStaged(packageName)
        staging / packageName
    }

    suspend fun discardStaged(packageName: String) = withContext(Dispatchers.IO) {
        artifacts(staging, packageName).forEach { it.deleteRecursively() }
    }

    suspend fun clearStaging() = withContext(Dispatchers.IO) {
        if (staging.exists()) staging.list().forEach { it.deleteRecursively() }
    }

    /** The engine only overwrites the splits it writes, so both previous output forms are deleted first. */
    suspend fun publish(packageName: String, staged: PlatformFile): PlatformFile = withContext(Dispatchers.IO) {
        val stem = "$packageName.reseamed"
        artifacts(directory, stem).forEach { it.deleteRecursively() }
        val output = if (staged.isDirectory()) directory / stem else directory / "$stem.apk"
        staged.atomicMove(output)
        output
    }

    suspend fun save(app: PatchedApp) {
        val icons = directory / "icons"
        val icon = icons / app.packageName
        val iconPath = app.iconPath?.let { source ->
            withContext(Dispatchers.IO) {
                if (source != icon.absolutePath()) {
                    icons.createDirectories()
                    PlatformFile(source).copyTo(icon)
                }
            }
            icon.absolutePath()
        }
        store.update { it.copy(apps = it.apps.filterNot { existing -> existing.packageName == app.packageName } + app.copy(iconPath = iconPath)) }
    }

    suspend fun markInstalled(packageName: String, installedAs: String) {
        store.update { library -> library.copy(apps = library.apps.map { if (it.packageName == packageName) it.copy(installedAs = installedAs) else it }) }
    }

    suspend fun remove(packageName: String) {
        val app = store.state.value.apps.firstOrNull { it.packageName == packageName } ?: return
        withContext(Dispatchers.IO) {
            artifacts(directory, "$packageName.reseamed").forEach { it.deleteRecursively() }
            app.iconPath?.let { PlatformFile(it).delete(mustExist = false) }
        }
        store.update { it.copy(apps = it.apps - app) }
    }
}

private suspend fun PlatformFile.deleteRecursively() {
    if (!exists()) return
    if (isDirectory()) list().forEach { it.deleteRecursively() }
    delete(mustExist = false)
}

private fun artifacts(parent: PlatformFile, stem: String) = listOf(parent / stem, parent / "$stem.apk")
