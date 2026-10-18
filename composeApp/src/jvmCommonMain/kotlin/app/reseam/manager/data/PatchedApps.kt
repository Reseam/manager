package app.reseam.manager.data

import app.reseam.manager.sdk.SelectionSerializer
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.PatchSelection
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

/** A library entry: an app keeps one build per install method, since a mount and an install reach different places. */
data class PatchedAppKey(val packageName: String, val installMethod: InstallMethod) {
    /** Never the `<package>.reseamed` of libraries that kept one build per app, so their files can't be overwritten by another build. */
    val stem: String get() = "$packageName-reseamed-${installMethod.name.lowercase()}"
}

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
    val source: AppSource? = null,
    @Serializable(with = SelectionSerializer::class) val selection: PatchSelection? = null,
    val bundles: List<BundleRef> = emptyList(),
) {
    val key: PatchedAppKey get() = PatchedAppKey(packageName, installMethod)
}

@Serializable
data class PatchedAppLibrary(val apps: List<PatchedApp> = emptyList())

class PatchedAppRepository(
    private val store: JsonStore<PatchedAppLibrary>,
    private val directory: PlatformFile,
) {
    private val staging = directory / "staging"

    val apps: Flow<List<PatchedApp>> = store.state.map { it.apps }

    /** The app's builds, newest first. */
    fun builds(packageName: String): Flow<List<PatchedApp>> =
        apps.map { list -> list.filter { it.packageName == packageName }.sortedByDescending { it.patchedAtEpochMs } }

    fun find(key: PatchedAppKey): Flow<PatchedApp?> = apps.map { list -> list.firstOrNull { it.key == key } }

    /** Runs write here, so a failed run leaves the previous output intact. */
    suspend fun stage(key: PatchedAppKey): PlatformFile = withContext(Dispatchers.IO) {
        discardStaged(key)
        staging / key.stem
    }

    suspend fun discardStaged(key: PatchedAppKey) = withContext(Dispatchers.IO) {
        artifacts(staging, key.stem).forEach { it.deleteRecursively() }
    }

    suspend fun clearStaging() = withContext(Dispatchers.IO) {
        if (staging.exists()) staging.list().forEach { it.deleteRecursively() }
    }

    /** The engine only overwrites the splits it writes, so both previous output forms are deleted first. */
    suspend fun publish(key: PatchedAppKey, staged: PlatformFile): PlatformFile = withContext(Dispatchers.IO) {
        artifacts(directory, key.stem).forEach { it.deleteRecursively() }
        val output = if (staged.isDirectory()) directory / key.stem else directory / "${key.stem}.apk"
        staged.atomicMove(output)
        output
    }

    /** Takes the place of the build with the same key, deleting its output when it was kept elsewhere. */
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
        val replaced = store.state.value.apps.firstOrNull { it.key == app.key }
        store.update { it.copy(apps = it.apps.filterNot { existing -> existing.key == app.key } + app.copy(iconPath = iconPath)) }
        if (replaced != null && replaced.apkPath != app.apkPath) withContext(Dispatchers.IO) { PlatformFile(replaced.apkPath).deleteRecursively() }
    }

    suspend fun markInstalled(key: PatchedAppKey, installedAs: String) {
        store.update { library -> library.copy(apps = library.apps.map { if (it.key == key) it.copy(installedAs = installedAs) else it }) }
    }

    /** The icon is shared by the app's builds, so it goes with the last one. */
    suspend fun remove(key: PatchedAppKey) {
        val apps = store.state.value.apps
        val app = apps.firstOrNull { it.key == key } ?: return
        withContext(Dispatchers.IO) {
            PlatformFile(app.apkPath).deleteRecursively()
            if (apps.none { it != app && it.packageName == app.packageName }) app.iconPath?.let { PlatformFile(it).delete(mustExist = false) }
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
