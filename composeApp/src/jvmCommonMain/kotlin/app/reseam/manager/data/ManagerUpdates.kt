package app.reseam.manager.data

import app.reseam.manager.ManagerVersion
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.ArtifactOutcome
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.HttpStatusException
import app.reseam.manager.platform.httpDownload
import app.reseam.manager.userMessage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ManagerUpdatePhase {
    data object Available : ManagerUpdatePhase
    data class Downloading(val written: Long, val total: Long?) : ManagerUpdatePhase
    data object Installing : ManagerUpdatePhase
    data class Failed(val message: String) : ManagerUpdatePhase
}

data class ManagerUpdate(val version: String, val releaseUrl: String, val phase: ManagerUpdatePhase = ManagerUpdatePhase.Available)

/** Progress moves in 256 KiB steps so a download doesn't recompose on every 64 KiB chunk. */
private const val ProgressStep = 256L * 1024

/** The ABI splits in build.gradle.kts, each published as its own release APK. */
private val ReleaseAbis = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

class ManagerUpdater(
    private val scope: CoroutineScope,
    private val settings: SettingsRepository,
    private val directory: PlatformFile,
    device: DeviceProfile?,
    private val installer: ArtifactAction,
) {
    private val current = MutableStateFlow<ManagerUpdate?>(null)
    val update: StateFlow<ManagerUpdate?> = current.asStateFlow()

    /** Null where Manager does not ship as an APK, so the update comes from the release page. */
    private val abi = device?.abis?.firstOrNull { it in ReleaseAbis }
    val installable: Boolean get() = abi != null

    private var installing: Job? = null

    /** Silent on failure: an unreachable or empty index is not something the user can act on. */
    fun check() {
        scope.launch {
            val release = runCatching { fetchManagerRelease(settings.settings.value.apiBaseUrl) }.getOrNull() ?: return@launch
            if (isNewerVersion(release.version, ManagerVersion)) {
                current.value = ManagerUpdate(release.version, release.downloadUrl)
            } else {
                current.value = null
                withContext(Dispatchers.IO) { if (directory.exists()) directory.list().forEach { it.delete(mustExist = false) } }
            }
        }
    }

    /** The APK stays cached, so a retry after granting install permission starts the installer straight away. */
    fun install() {
        val update = current.value ?: return
        val abi = abi ?: return
        if (installing?.isActive == true) return
        installing = scope.launch {
            val apk = directory / "reseam-manager-${update.version}-$abi.apk"
            val staged = directory / "reseam-manager-${update.version}-$abi.apk.part"
            try {
                if (!apk.exists()) {
                    current.value = update.copy(phase = ManagerUpdatePhase.Downloading(0, null))
                    withContext(Dispatchers.IO) { directory.createDirectories() }
                    httpDownload(apkUrl(settings.settings.value.apiBaseUrl, update.version, abi), staged) { written, total ->
                        current.value = update.copy(phase = ManagerUpdatePhase.Downloading(written / ProgressStep * ProgressStep, total))
                    }
                    withContext(Dispatchers.IO) { staged.atomicMove(apk) }
                }
                current.value = update.copy(phase = ManagerUpdatePhase.Installing)
                current.value = when (val outcome = installer.run(apk)) {
                    is ArtifactOutcome.Failed -> update.copy(phase = ManagerUpdatePhase.Failed("The install failed: ${outcome.message}"))
                    else -> update
                }
            } catch (cancelled: CancellationException) {
                current.value = update
                withContext(NonCancellable + Dispatchers.IO) { staged.delete(mustExist = false) }
                throw cancelled
            } catch (error: Exception) {
                withContext(Dispatchers.IO) { staged.delete(mustExist = false) }
                current.value = update.copy(phase = ManagerUpdatePhase.Failed(error.updateMessage()))
            }
        }
    }

    fun cancel() {
        installing?.cancel()
    }
}

/** Release assets sit behind the API's `/manager/<tag>/<name>` redirect, at the API host's root. */
internal fun apkUrl(apiBaseUrl: String, version: String, abi: String): String =
    URI(apiBaseUrl).resolve("/manager/v$version/composeApp-$abi-release.apk").toString()

private fun Throwable.updateMessage(): String = when (this) {
    is HttpStatusException -> "The server refused the download (HTTP $status). Try again later."
    is IOException -> "The download stopped. Check your connection and try again."
    else -> userMessage()
}
