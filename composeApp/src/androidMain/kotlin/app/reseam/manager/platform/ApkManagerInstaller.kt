package app.reseam.manager.platform

import android.os.Build
import app.reseam.manager.data.Failure
import io.github.vinceglb.filekit.PlatformFile

private val ReleaseAbis = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

class ApkManagerInstaller private constructor(private val abi: String, private val installer: AndroidInstaller) : ManagerInstaller {
    override fun fileName(version: String) = "reseam-manager-$version-$abi.apk"

    override suspend fun install(file: PlatformFile) {
        val outcome = installer.run(file, useSystemInstaller = false)
        if (outcome is ArtifactOutcome.Failed) throw Failure.InstallFailed(outcome.reason, outcome.detail)
    }

    companion object {
        /** Null on a device whose ABIs no release is built for. */
        fun forDevice(installer: AndroidInstaller): ApkManagerInstaller? =
            Build.SUPPORTED_ABIS.firstOrNull { it in ReleaseAbis }?.let { ApkManagerInstaller(it, installer) }
    }
}
