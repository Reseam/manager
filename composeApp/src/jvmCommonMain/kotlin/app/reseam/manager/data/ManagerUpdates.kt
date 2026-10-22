package app.reseam.manager.data

import app.reseam.manager.ManagerVersion
import app.reseam.manager.platform.ManagerInstaller
import app.reseam.manager.platform.Transfer
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
    data class Downloading(val transfer: Transfer) : ManagerUpdatePhase
    data object Installing : ManagerUpdatePhase
    data class Failed(val error: Exception) : ManagerUpdatePhase
}

data class ManagerUpdate(val version: String, val releaseUrl: String, val phase: ManagerUpdatePhase = ManagerUpdatePhase.Available)

class ManagerUpdater(
    private val scope: CoroutineScope,
    private val settings: SettingsRepository,
    private val directory: PlatformFile,
    private val installer: ManagerInstaller?,
) {
    private val current = MutableStateFlow<ManagerUpdate?>(null)
    val update: StateFlow<ManagerUpdate?> = current.asStateFlow()

    val installable: Boolean get() = installer != null

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
        val installer = installer ?: return
        if (installing?.isActive == true) return
        installing = scope.launch {
            val name = installer.fileName(update.version)
            val file = directory / name
            val staged = directory / "$name.part"
            try {
                if (!file.exists()) {
                    current.value = update.copy(phase = ManagerUpdatePhase.Downloading(Transfer(0, null)))
                    withContext(Dispatchers.IO) { directory.createDirectories() }
                    httpDownload(releaseFileUrl(settings.settings.value.apiBaseUrl, update.version, name), staged) { current.value = update.copy(phase = ManagerUpdatePhase.Downloading(it)) }
                    withContext(Dispatchers.IO) { staged.atomicMove(file) }
                }
                current.value = update.copy(phase = ManagerUpdatePhase.Installing)
                installer.install(file)
                current.value = update
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

private fun releaseFileUrl(apiBaseUrl: String, version: String, name: String): String =
    URI(apiBaseUrl).resolve("/manager/v$version/$name").toString()
