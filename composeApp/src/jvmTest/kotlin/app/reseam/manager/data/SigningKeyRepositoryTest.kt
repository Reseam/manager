package app.reseam.manager.data

import app.reseam.manager.platform.decodeKeystore
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import kotlinx.coroutines.test.runTest
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SigningKeyRepositoryTest {
    private val fixtureKey = fixture("test.pk8")
    private val fixtureCert = fixture("test.der")

    private fun repository(seeded: Boolean): Pair<SigningKeyRepository, File> {
        val directory = createTempDirectory("reseam-signing-test").toFile()
        if (seeded) {
            directory.resolve("reseam.pk8").writeBytes(fixtureKey)
            directory.resolve("reseam.der").writeBytes(fixtureCert)
        }
        return SigningKeyRepository(PlatformFile(directory)) to directory
    }

    @Test
    fun filesPointWhereTheEngineWritesAndReadsTheKey() {
        val (repository, directory) = repository(seeded = false)
        assertNull(repository.info.value)
        val files = repository.files()
        assertEquals(directory.resolve("reseam.pk8").absolutePath, files.key)
        assertEquals(directory.resolve("reseam.der").absolutePath, files.cert)
        assertTrue(directory.isDirectory)
    }

    @Test
    fun exportedKeystoreImportsIntoAnotherInstallWithTheSameIdentity() = runTest {
        val (source, _) = repository(seeded = true)
        val fingerprint = assertNotNull(source.info.value).fingerprint
        assertEquals(95, fingerprint.length)

        val keystore = createTempDirectory("reseam-keystore").toFile().resolve("reseam.p12")
        source.export(PlatformFile(keystore), "correct horse".toCharArray())

        val (target, directory) = repository(seeded = false)
        target.import(PlatformFile(keystore), "correct horse".toCharArray())
        assertEquals(fingerprint, assertNotNull(target.info.value).fingerprint)
        assertContentEquals(fixtureCert, directory.resolve("reseam.der").readBytes())
        assertEquals(scalar(fixtureKey), scalar(directory.resolve("reseam.pk8").readBytes()))
    }

    @Test
    fun importedKeyCarriesThePublicPointTheEngineRequires() = runTest {
        val (source, _) = repository(seeded = true)
        val keystore = createTempDirectory("reseam-keystore").toFile().resolve("reseam.p12")
        source.export(PlatformFile(keystore), "pw".toCharArray())
        val material = decodeKeystore(keystore.readBytes(), "pw".toCharArray())

        // PrivateKeyInfo { 0, { ecPublicKey, prime256v1 }, ECPrivateKey { 1, scalar, [1] point } }
        val ecPublicKey = byteArrayOf(0x06, 0x07, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01)
        val prime256v1 = byteArrayOf(0x06, 0x08, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07)
        val encoded = material.privateKeyPkcs8
        assertTrue(encoded.indexOf(ecPublicKey) > 0)
        assertTrue(encoded.indexOf(prime256v1) > 0)
        val point = encoded.indexOf(byteArrayOf(0xA1.toByte(), 0x44, 0x03, 0x42, 0x00, 0x04))
        assertTrue(point > 0, "public point tagged [1] missing")
        assertEquals(encoded.size, point + 6 + 64)
    }

    @Test
    fun wrongPasswordAndForeignKeysAreRejected() = runTest {
        val (source, _) = repository(seeded = true)
        val keystore = createTempDirectory("reseam-keystore").toFile().resolve("reseam.p12")
        source.export(PlatformFile(keystore), "right".toCharArray())
        val (target, _) = repository(seeded = false)
        assertFailsWith<IllegalStateException> { target.import(PlatformFile(keystore), "wrong".toCharArray()) }
        assertNull(target.info.value)
    }

    @Test
    fun resetForgetsTheKeySoTheEngineGeneratesAFreshOne() = runTest {
        val (repository, directory) = repository(seeded = true)
        repository.reset()
        assertNull(repository.info.value)
        assertTrue((PlatformFile(directory) / "reseam.pk8").let { !File(it.toString()).exists() })
    }

    private fun scalar(pkcs8: ByteArray) = (KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(pkcs8)) as ECPrivateKey).s

    private fun fixture(name: String): ByteArray = checkNotNull(javaClass.getResourceAsStream("/signing/$name")) { "missing fixture $name" }.readBytes()

    private fun ByteArray.indexOf(needle: ByteArray): Int =
        (0..size - needle.size).firstOrNull { offset -> needle.indices.all { this[offset + it] == needle[it] } } ?: -1
}
