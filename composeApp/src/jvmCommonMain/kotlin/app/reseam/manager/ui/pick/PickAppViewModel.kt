package app.reseam.manager.ui.pick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.SavedApk
import app.reseam.manager.data.SavedApkOrigin
import app.reseam.manager.platform.InstalledApp
import app.reseam.manager.platform.enginePath
import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.hidden
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.download.VersionOption
import app.reseam.manager.ui.download.versionOptions
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.userMessage
import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PickMode { Apps, File }

data class InstalledCandidate(val app: InstalledApp, val patchCount: Int)

data class SavedCandidate(val apk: SavedApk, val patchCount: Int)

data class DownloadCandidate(val packageName: String, val patchCount: Int, val versions: List<VersionOption>)

data class PickCatalog(val installed: List<InstalledCandidate>, val saved: List<SavedCandidate>, val downloadable: List<DownloadCandidate>)

fun InstalledApp.target() = PatchTarget(name, packageName, versionName, apkPath, splitPaths)

/**
 * The installed [app] to patch. While Reseam has the app mounted, its paths show the patched APKs, so the app's own
 * APKs are copied out from under the mount.
 */
suspend fun AppGraph.installedTarget(app: InstalledApp, installMethod: InstallMethod): PatchTarget {
    val target = app.target().copy(installMethod = installMethod)
    val mounter = mounter ?: return target
    if (patchedApps.find(app.packageName).first()?.installMethod != InstallMethod.MOUNT || !mounter.isMounted(app.packageName)) return target
    val originals = mounter.copyOriginals(app.packageName, cacheDirectory / "originals" / app.packageName)
    return target.copy(apkPath = originals.first().absolutePath(), splitPaths = originals.drop(1).map { it.absolutePath() })
}

fun SavedApk.target() = PatchTarget(name, packageName, versionName, path, iconPath = iconPath)

data class PickAppState(
    val mode: PickMode = PickMode.Apps,
    val query: String = "",
    val selected: PatchTarget? = null,
    val pickingFile: Boolean = false,
    val showingAll: Boolean = false,
)

class PickAppViewModel(private val graph: AppGraph) : ViewModel() {
    val installedSupported: Boolean = graph.installedApps != null

    private val preparingState = MutableStateFlow(false)

    /** The installed app sheet is preparing its target. */
    val preparing: StateFlow<Boolean> = preparingState.asStateFlow()

    private val current = MutableStateFlow(PickAppState())
    val state: StateFlow<PickAppState> = current.asStateFlow()

    /** Patches that apply to any app; they make every installed app a candidate. */
    val universalCount: StateFlow<Int> = graph.bundles.patches
        .map { loaded -> loaded.orEmpty().values.flatten().count { !it.hidden && it.universal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Null while loading. */
    val catalog: StateFlow<PickCatalog?> = combine(graph.bundles.patches, graph.savedApks.apks) { loaded, savedApks ->
        val patches = loaded?.values?.flatten()?.filter { !it.hidden } ?: return@combine null
        val universal = patches.count { it.universal }
        val declared = patches.flatMap { it.declared }.groupBy { it.`package` }
        val installed = graph.installedApps?.query(declared.keys).orEmpty()
        val installedPackages = installed.map { it.packageName }.toSet()
        PickCatalog(
            installed = installed.map { InstalledCandidate(it, declared.getValue(it.packageName).size + universal) }
                .sortedWith(compareByDescending<InstalledCandidate> { it.patchCount }.thenBy { it.app.name.lowercase() }),
            saved = savedApks.map { SavedCandidate(it, declared[it.packageName].orEmpty().size + universal) }
                .sortedWith(compareByDescending<SavedCandidate> { it.patchCount }.thenBy { it.apk.name.lowercase() }),
            downloadable = declared.filterKeys { it !in installedPackages }
                .map { (packageName, entries) -> DownloadCandidate(packageName, entries.size + universal, versionOptions(entries, universal)) }
                .sortedWith(compareByDescending<DownloadCandidate> { it.patchCount }.thenBy { it.packageName }),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val everything = MutableStateFlow<List<InstalledApp>?>(null)

    /** The rest of the device's apps once [showAllApps] was asked for. Null while loading. */
    val others: StateFlow<List<InstalledCandidate>?> = combine(everything, catalog, universalCount) { all, catalog, universal ->
        val known = catalog?.installed.orEmpty().map { it.app.packageName }.toSet()
        all?.filter { it.packageName !in known }?.map { InstalledCandidate(it, universal) }?.sortedBy { it.app.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun showAllApps() {
        current.update { it.copy(showingAll = true) }
        if (everything.value != null) return
        viewModelScope.launch {
            everything.value = runCatching { graph.installedApps?.all().orEmpty() }
                .getOrElse { error -> graph.notices.warn(error.userMessage()); emptyList() }
        }
    }

    fun use(app: InstalledApp, onTarget: (PatchTarget) -> Unit) {
        if (preparingState.value) return
        preparingState.value = true
        viewModelScope.launch {
            runCatching { onTarget(graph.installedTarget(app, InstallMethod.INSTALL)) }.onFailure { graph.notices.warn(it.userMessage()) }
            preparingState.value = false
        }
    }

    fun setMode(mode: PickMode) = current.update { if (it.pickingFile) it else it.copy(mode = mode, selected = null) }

    fun setQuery(query: String) = current.update { it.copy(query = query) }

    fun pickFile(launch: () -> Unit) {
        if (current.value.pickingFile) return
        current.update { it.copy(pickingFile = true) }
        try {
            launch()
        } catch (error: Exception) {
            onFilePicked(Result.failure(error))
        }
    }

    fun onFilePicked(result: Result<PlatformFile?>) {
        viewModelScope.launch {
            try {
                val picked = result.getOrThrow() ?: return@launch
                val target = picked.enginePath
                    ?.let { path -> graph.appIdentities.read(path).let { PatchTarget(it.name, it.packageName, it.versionName, path, iconPath = it.iconPath) } }
                    ?: graph.savedApks.save(picked.name.substringAfterLast('.', "apk"), SavedApkOrigin.File) { withContext(Dispatchers.IO) { picked.copyTo(it) } }.target()
                current.update { it.copy(selected = target) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                graph.notices.warn(error.userMessage())
            } finally {
                current.update { it.copy(pickingFile = false) }
            }
        }
    }

}

fun List<InstalledCandidate>.matching(query: String): List<InstalledCandidate> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return this
    return filter { it.app.name.lowercase().contains(needle) || it.app.packageName.contains(needle) }
}

@JvmName("matchingDownloads")
fun List<DownloadCandidate>.matching(query: String): List<DownloadCandidate> {
    val needle = query.trim().lowercase()
    return if (needle.isEmpty()) this else filter { it.packageName.contains(needle) }
}

@JvmName("matchingSaved")
fun List<SavedCandidate>.matching(query: String): List<SavedCandidate> {
    val needle = query.trim().lowercase()
    return if (needle.isEmpty()) this else filter { it.apk.name.lowercase().contains(needle) || it.apk.packageName.orEmpty().contains(needle) }
}
