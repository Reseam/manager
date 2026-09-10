package app.reseam.manager.platform

/** An APK signing identity in the engine's file formats: a PKCS#8 ECDSA P-256 key and its DER X.509 certificate. */
class SigningMaterial(val privateKeyPkcs8: ByteArray, val certificateDer: ByteArray)

/** A PKCS#12 keystore holding [material] under one alias, protected by [password]. */
expect fun encodeKeystore(material: SigningMaterial, password: CharArray): ByteArray

/** The signing identity in a PKCS#12 or platform keystore. Fails for keys the engine cannot sign with. */
expect fun decodeKeystore(bytes: ByteArray, password: CharArray): SigningMaterial

/** SHA-256 of the certificate, hex pairs separated by colons. */
expect fun certificateFingerprint(certificateDer: ByteArray): String
