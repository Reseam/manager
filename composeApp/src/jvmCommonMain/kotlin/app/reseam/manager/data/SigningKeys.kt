package app.reseam.manager.data

import app.reseam.sdk.SigningIdentity
import app.reseam.sdk.SigningKeyMaterial
import app.reseam.sdk.certificateFingerprint
import app.reseam.sdk.exportPkcs12
import app.reseam.sdk.importPkcs12
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readByteArray

data class SigningKeyInfo(val fingerprint: String)

class SigningKeyRepository(private val directory: PlatformFile) {
    private val keyFile = directory / "reseam.pk8"
    private val certFile = directory / "reseam.der"
    private val current = MutableStateFlow(read())

    val info: StateFlow<SigningKeyInfo?> = current.asStateFlow()

    fun identity(): SigningIdentity {
        directory.createDirectories()
        return SigningIdentity.Files(key = keyFile.absolutePath(), cert = certFile.absolutePath())
    }

    fun refresh() {
        current.value = read()
    }

    /** Backs the key up as a PKCS#12 keystore, which every Reseam app imports. */
    suspend fun export(into: PlatformFile, password: String) = withContext(Dispatchers.IO) {
        into.writeBytes(exportPkcs12(SigningKeyMaterial(keyFile.readBytes(), certFile.readBytes()), password))
    }

    suspend fun import(from: PlatformFile, password: String) = withContext(Dispatchers.IO) {
        val material = importPkcs12(from.readBytes(), password)
        directory.createDirectories()
        replace(keyFile, material.key)
        replace(certFile, material.cert)
        refresh()
    }

    suspend fun reset() = withContext(Dispatchers.IO) {
        keyFile.delete(mustExist = false)
        certFile.delete(mustExist = false)
        refresh()
    }

    private fun read(): SigningKeyInfo? =
        if (keyFile.exists() && certFile.exists()) SigningKeyInfo(certificateFingerprint(certFile.readBytes())) else null

    private suspend fun replace(file: PlatformFile, bytes: ByteArray) {
        val staging = directory / "${file.name}.tmp"
        staging.writeBytes(bytes)
        staging.atomicMove(file)
    }
}

private fun PlatformFile.readBytes(): ByteArray = source().buffered().use { it.readByteArray() }

private fun PlatformFile.writeBytes(bytes: ByteArray) = sink().buffered().use { it.write(bytes) }
