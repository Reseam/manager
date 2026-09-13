package app.reseam.manager.platform

/** An APK signing identity in the engine's file formats: a PKCS#8 ECDSA P-256 key and its DER X.509 certificate. */
class SigningMaterial(val privateKeyPkcs8: ByteArray, val certificateDer: ByteArray)
