package app.reseam.manager.platform

import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec

private const val Alias = "reseam"
private const val CoordinateBytes = 32

actual fun encodeKeystore(material: SigningMaterial, password: CharArray): ByteArray {
    val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(material.privateKeyPkcs8))
    val certificate = CertificateFactory.getInstance("X.509").generateCertificate(material.certificateDer.inputStream())
    val store = KeyStore.getInstance("PKCS12").apply {
        load(null, null)
        setKeyEntry(Alias, key, password, arrayOf(certificate))
    }
    return ByteArrayOutputStream().use { output ->
        store.store(output, password)
        output.toByteArray()
    }
}

actual fun decodeKeystore(bytes: ByteArray, password: CharArray): SigningMaterial {
    val store = openKeystore(bytes, password)
    val alias = store.aliases().asSequence().firstOrNull(store::isKeyEntry) ?: error("The keystore holds no private key")
    val key = store.getKey(alias, password) as? ECPrivateKey ?: error("The key under '$alias' is not an EC key; Reseam signs with ECDSA P-256")
    check(key.params.curve.field.fieldSize == CoordinateBytes * 8) { "The key under '$alias' is not on the P-256 curve; Reseam signs with ECDSA P-256" }
    val certificate = store.getCertificate(alias) as X509Certificate
    val public = certificate.publicKey as? ECPublicKey ?: error("The certificate under '$alias' does not carry an EC public key")
    return SigningMaterial(pkcs8(key, public), certificate.encoded)
}

actual fun certificateFingerprint(certificateDer: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(certificateDer).joinToString(":") { "%02X".format(it) }

/** PKCS#12 first, then the platform's own type, which is what `keytool` writes by default on the JVM. */
private fun openKeystore(bytes: ByteArray, password: CharArray): KeyStore {
    val types = listOf("PKCS12", KeyStore.getDefaultType()).distinct()
    var failure: Exception? = null
    for (type in types) {
        try {
            return KeyStore.getInstance(type).apply { load(bytes.inputStream(), password) }
        } catch (error: Exception) {
            failure = error
        }
    }
    throw IllegalStateException("Could not open the keystore: wrong password or unsupported format", failure)
}

/**
 * The engine reads PKCS#8 v1 whose ECPrivateKey carries the public point, which the JDK's own
 * encoding omits, so the key is re-encoded from its scalar and the certificate's public key.
 */
private fun pkcs8(key: ECPrivateKey, public: ECPublicKey): ByteArray {
    val point = byteArrayOf(0x04) + fixed(public.w.affineX.toByteArray()) + fixed(public.w.affineY.toByteArray())
    val ecPrivateKey = sequence(
        integer(1),
        octetString(fixed(key.s.toByteArray())),
        tagged(1, bitString(point)),
    )
    return sequence(
        integer(0),
        sequence(EcPublicKeyOid, Prime256v1Oid),
        octetString(ecPrivateKey),
    )
}

private val EcPublicKeyOid = byteArrayOf(0x06, 0x07, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x02, 0x01)
private val Prime256v1Oid = byteArrayOf(0x06, 0x08, 0x2A, 0x86.toByte(), 0x48, 0xCE.toByte(), 0x3D, 0x03, 0x01, 0x07)

/** A big-endian coordinate at the curve's width: BigInteger drops leading zeros and may add a sign byte. */
private fun fixed(value: ByteArray): ByteArray {
    val stripped = value.dropWhile { it == 0.toByte() }.toByteArray()
    check(stripped.size <= CoordinateBytes) { "EC value wider than the P-256 field" }
    return ByteArray(CoordinateBytes - stripped.size) + stripped
}

private fun der(tag: Int, content: ByteArray): ByteArray {
    val length = when {
        content.size < 0x80 -> byteArrayOf(content.size.toByte())
        content.size < 0x100 -> byteArrayOf(0x81.toByte(), content.size.toByte())
        else -> byteArrayOf(0x82.toByte(), (content.size shr 8).toByte(), content.size.toByte())
    }
    return byteArrayOf(tag.toByte()) + length + content
}

private fun sequence(vararg parts: ByteArray) = der(0x30, parts.fold(ByteArray(0), ByteArray::plus))
private fun integer(value: Int) = der(0x02, byteArrayOf(value.toByte()))
private fun octetString(bytes: ByteArray) = der(0x04, bytes)
private fun bitString(bytes: ByteArray) = der(0x03, byteArrayOf(0) + bytes)
private fun tagged(number: Int, inner: ByteArray) = der(0xA0 or number, inner)
