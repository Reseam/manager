package app.reseam.manager.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Build
import app.reseam.manager.data.BuildContainer
import app.reseam.manager.data.HumanCheckRequired
import app.reseam.manager.data.SavedApkOrigin
import app.reseam.manager.data.SourceBuild
import app.reseam.manager.data.best
import app.reseam.manager.data.fits
import app.reseam.manager.data.isNewerVersion
import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.hidden
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.pick.target
import app.reseam.manager.userMessage
import app.reseam.sdk.CompatiblePackage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** A null [version] is the newest stable release. */
data class VersionOption(val version: String?, val patchCount: Int)

sealed interface DownloadPhase {
    data object Resolving : DownloadPhase
    data object Ready : DownloadPhase
    data object Preparing : DownloadPhase
    data class Downloading(val fraction: Float?) : DownloadPhase
    data class Failed(val message: String) : DownloadPhase
}

data class DownloadState(
    val appName: String? = null,
    val patchCount: Int = 0,
    val versions: List<VersionOption> = emptyList(),
    val version: VersionOption? = null,
    val builds: List<Build> = emptyList(),
    val recommended: Build? = null,
    val build: Build? = null,
    val phase: DownloadPhase = DownloadPhase.Resolving,
    val verification: String? = null,
)

private val NewestFirst = Comparator<String> { a, b -> if (isNewerVersion(a, b)) -1 else if (isNewerVersion(b, a)) 1 else 0 }

class DownloadViewModel(private val graph: AppGraph, private val packageName: String) : ViewModel() {
    private val current = MutableStateFlow(DownloadState())
    val state: StateFlow<DownloadState> = current.asStateFlow()

    val saved: StateFlow<Set<String>> = graph.savedApks.apks
        .map { apks -> apks.mapNotNull { apk -> apk.sourceBuild?.takeIf { it.source == graph.downloader.source }?.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var job: Job? = null
    private var step: (suspend () -> Unit)? = null

    init {
        launchStep(::prepare)
    }

    fun fits(build: Build): Boolean = graph.device?.let(build::fits) ?: true

    fun chooseVersion(option: VersionOption) {
        current.update { it.copy(version = option, builds = emptyList(), recommended = null, build = null) }
        launchStep(::resolve)
    }

    fun chooseBuild(build: Build) = current.update { it.copy(build = build) }

    fun download(onDownloaded: (PatchTarget) -> Unit) = launchStep { fetch(onDownloaded) }

    fun retry() {
        step?.let(::launchStep)
    }

    fun dismissVerification() = current.update {
        it.copy(verification = null, phase = DownloadPhase.Failed("The check didn't finish, so nothing was downloaded."))
    }

    private fun launchStep(next: suspend () -> Unit) {
        step = next
        job?.cancel()
        current.update { it.copy(verification = null) }
        job = viewModelScope.launch {
            try {
                next()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (challenge: HumanCheckRequired) {
                current.update { it.copy(verification = challenge.url) }
            } catch (error: Exception) {
                current.update { it.copy(phase = DownloadPhase.Failed(error.userMessage())) }
            }
        }
    }

    private suspend fun prepare() {
        if (current.value.versions.isEmpty()) {
            val patches = graph.bundles.patches.filterNotNull().first().values.flatten().filter { !it.hidden }
            val universal = patches.count { it.universal }
            val entries = patches.flatMap { it.declared }.filter { it.`package` == packageName }
            val versions = versionOptions(entries, universal)
            current.update { it.copy(patchCount = entries.size + universal, versions = versions, version = versions.first()) }
        }
        resolve()
    }

    private suspend fun resolve() {
        current.update { it.copy(phase = DownloadPhase.Resolving) }
        val (app, builds) = graph.downloader.builds(packageName, current.value.version?.version)
        val recommended = builds.best(graph.device)
        current.update {
            it.copy(appName = app.name, phase = DownloadPhase.Ready, builds = builds, recommended = recommended, build = recommended)
        }
    }

    /** The download is only as trustworthy as what resolved it, so the file has to say it is the right app. */
    private suspend fun fetch(onDownloaded: (PatchTarget) -> Unit) {
        val build = current.value.build ?: return
        val downloader = graph.downloader
        val sourceBuild = SourceBuild(downloader.source, build.id)
        val apk = graph.savedApks.downloaded(sourceBuild) ?: run {
            current.update { it.copy(phase = DownloadPhase.Preparing) }
            val target = downloader.resolve(build)
            current.update { it.copy(phase = DownloadPhase.Downloading(fraction = null)) }
            val extension = when (build.container) {
                BuildContainer.Apk -> "apk"
                BuildContainer.Bundle -> "apkm"
            }
            graph.savedApks.save(extension, SavedApkOrigin.Download, sourceBuild) { file ->
                downloader.download(target, file) { written, total ->
                    if (total != null) current.update { it.copy(phase = DownloadPhase.Downloading(fraction = (written * 100 / total) / 100f)) }
                }
            }
        }
        if (apk.packageName != packageName) {
            graph.savedApks.remove(apk.id)
            throw IOException("The download was ${apk.packageName}, not $packageName")
        }
        onDownloaded(apk.target())
    }
}

internal fun versionOptions(entries: List<CompatiblePackage>, universal: Int): List<VersionOption> {
    val anyVersion = entries.count { it.versions.isEmpty() } + universal
    val pinned = entries.flatMap { it.versions }.groupingBy { it }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy(NewestFirst) { it.key })
        .map { (version, count) -> VersionOption(version, count + anyVersion) }
    return pinned + listOfNotNull(VersionOption(null, anyVersion).takeIf { anyVersion > 0 || pinned.isEmpty() })
}
