package app.reseam.manager.data

import app.reseam.manager.platform.SigningMaterial
import app.reseam.manager.platform.certificateFingerprint
import app.reseam.manager.platform.decodeKeystore
import app.reseam.manager.platform.encodeKeystore
import app.reseam.sdk.SigningKeyFiles
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

    fun files(): SigningKeyFiles {
        directory.createDirectories()
        return SigningKeyFiles(key = keyFile.absolutePath(), cert = certFile.absolutePath())
    }

    fun refresh() {
        current.value = read()
    }

    suspend fun export(into: PlatformFile, password: CharArray) = withContext(Dispatchers.IO) {
        val material = SigningMaterial(keyFile.readBytes(), certFile.readBytes())
        into.writeBytes(encodeKeystore(material, password))
    }

    suspend fun import(from: PlatformFile, password: CharArray) = withContext(Dispatchers.IO) {
        val material = decodeKeystore(from.readBytes(), password)
        directory.createDirectories()
        replace(keyFile, material.privateKeyPkcs8)
        replace(certFile, material.certificateDer)
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
