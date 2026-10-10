package app.reseam.manager.data

import app.reseam.manager.ManagerVersion
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.ArtifactOutcome
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.httpDownload
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ManagerUpdatePhase {
    data object Available : ManagerUpdatePhase
    data class Downloading(val written: Long, val total: Long?) : ManagerUpdatePhase
    data object Installing : ManagerUpdatePhase
    data class Failed(val error: Exception) : ManagerUpdatePhase
}

data class ManagerUpdate(val version: String, val releaseUrl: String, val phase: ManagerUpdatePhase = ManagerUpdatePhase.Available)

/** Progress moves in 256 KiB steps so a download doesn't recompose on every 64 KiB chunk. */
private const val ProgressStep = 256L * 1024

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

    private val abi = device?.abis?.firstOrNull { it in ReleaseAbis }
    val installable: Boolean get() = abi != null

    private var installing: Job? = null

    suspend fun check(): ManagerUpdate? {
        val release = runCatching { fetchManagerRelease(settings.settings.value.apiBaseUrl) }.getOrNull() ?: return null
        if (isNewerVersion(release.version, ManagerVersion)) {
            return current.updateAndGet { it?.takeIf { it.version == release.version } ?: ManagerUpdate(release.version, release.downloadUrl) }
        }
        current.value = null
        withContext(Dispatchers.IO) { if (directory.exists()) directory.list().forEach { it.delete(mustExist = false) } }
        return null
    }

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
                current.value = when (val outcome = installer.run(apk, useSystemInstaller = false)) {
                    is ArtifactOutcome.Failed -> update.copy(phase = ManagerUpdatePhase.Failed(Failure.InstallFailed(outcome.reason, outcome.detail)))
                    else -> update
                }
            } catch (cancelled: CancellationException) {
                current.value = update
                withContext(NonCancellable + Dispatchers.IO) { staged.delete(mustExist = false) }
                throw cancelled
            } catch (error: Exception) {
                withContext(Dispatchers.IO) { staged.delete(mustExist = false) }
                current.value = update.copy(phase = ManagerUpdatePhase.Failed(error))
            }
        }
    }

    fun cancel() {
        installing?.cancel()
    }
}

private fun apkUrl(apiBaseUrl: String, version: String, abi: String): String =
    URI(apiBaseUrl).resolve("/manager/v$version/composeApp-$abi-release.apk").toString()
