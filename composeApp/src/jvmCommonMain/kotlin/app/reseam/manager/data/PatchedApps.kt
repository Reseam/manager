package app.reseam.manager.data

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

    /** A run writes here, apart from the installable output, so a failed run leaves the previous output intact. */
    suspend fun stage(packageName: String): PlatformFile = withContext(Dispatchers.IO) {
        discardStaged(packageName)
        staging / packageName
    }

    suspend fun discardStaged(packageName: String) = withContext(Dispatchers.IO) {
        artifacts(staging, packageName).forEach { it.deleteRecursively() }
    }

    /** Drops what runs cut short by a crash or a killed process left behind. */
    suspend fun clearStaging() = withContext(Dispatchers.IO) {
        if (staging.exists()) staging.list().forEach { it.deleteRecursively() }
    }

    /**
     * Replaces the package's previous output, as a single APK or a split directory, with [staged]. The engine only
     * overwrites the splits it writes, so a leftover split from an earlier run would otherwise be installed with the new ones.
     */
    suspend fun publish(packageName: String, staged: PlatformFile): PlatformFile = withContext(Dispatchers.IO) {
        val stem = "$packageName.reseamed"
        artifacts(directory, stem).forEach { it.deleteRecursively() }
        val output = if (staged.isDirectory()) directory / stem else directory / "$stem.apk"
        staged.atomicMove(output)
        output
    }

    /** The library owns its icons: the picked file's icon is copied in, and a repatch reuses the copy. */
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

    suspend fun remove(packageName: String) {
        val app = store.state.value.apps.firstOrNull { it.packageName == packageName } ?: return
        store.update { it.copy(apps = it.apps - app) }
        withContext(Dispatchers.IO) {
            artifacts(directory, "$packageName.reseamed").forEach { it.deleteRecursively() }
            app.iconPath?.let { PlatformFile(it).delete(mustExist = false) }
        }
    }
}

private suspend fun PlatformFile.deleteRecursively() {
    if (isDirectory()) list().forEach { it.deleteRecursively() }
    delete(mustExist = false)
}

/** The engine appends `.apk` to an automatic output with a single component. */
private fun artifacts(parent: PlatformFile, stem: String) = listOf(parent / stem, parent / "$stem.apk")
