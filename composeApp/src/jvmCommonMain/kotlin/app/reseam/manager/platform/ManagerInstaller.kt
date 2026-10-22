package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile

/** Installs a downloaded Manager release over the running one. */
interface ManagerInstaller {
    /** The release file built for this device, as the release publishes it. */
    fun fileName(version: String): String

    /** Hands [file] to the system installer, throwing when it refuses it. */
    suspend fun install(file: PlatformFile)
}
