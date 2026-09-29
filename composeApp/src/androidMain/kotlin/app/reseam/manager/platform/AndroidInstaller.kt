package app.reseam.manager.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.path
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AndroidInstaller(private val context: Context) : ArtifactAction {
    override val label = "Install"

    override suspend fun run(artifact: PlatformFile): ArtifactOutcome {
        check(artifact.exists()) { "The patched APK is missing: ${artifact.path}" }
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return ArtifactOutcome.PermissionRequested
        }
        val apks = if (artifact.isDirectory()) {
            artifact.list().map { File(it.path) }.filter { it.extension.equals("apk", ignoreCase = true) }
        } else {
            listOf(File(artifact.path))
        }
        check(apks.isNotEmpty()) { "The patched split set has no APKs: ${artifact.path}" }
        return install(apks)
    }

    /** A session opens the system confirmation directly, where a view intent can land on an OEM store's chooser. */
    private suspend fun install(apks: List<File>): ArtifactOutcome = withContext(Dispatchers.IO) {
        val action = "app.reseam.manager.install.${System.nanoTime()}"
        val outcome = CompletableDeferred<InstallResult>()
        InstallResultReceiver.outcomes[action] = outcome
        try {
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, InstallResultReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            // Honored only when Manager updates itself or an app it installed; every other install still asks.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            val session = installer.openSession(installer.createSession(params))
            try {
                apks.forEach { apk ->
                    session.openWrite(apk.name, 0, apk.length()).use { stream ->
                        FileInputStream(apk).use { it.copyTo(stream) }
                        session.fsync(stream)
                    }
                }
                session.commit(pending.intentSender)
            } catch (error: Exception) {
                outcome.completeExceptionally(error)
                runCatching { session.abandon() }
            } finally {
                runCatching { session.close() }
            }
            val result = withTimeoutOrNull(120_000L) { outcome.await() }
                ?: return@withContext ArtifactOutcome.Failed("the installer did not answer within two minutes")
            when (result.status) {
                PackageInstaller.STATUS_SUCCESS -> ArtifactOutcome.Installed(checkNotNull(result.packageName) { "The installer reported success without a package name" })
                PackageInstaller.STATUS_FAILURE_ABORTED -> ArtifactOutcome.Cancelled
                else -> ArtifactOutcome.Failed(result.message ?: "status ${result.status}")
            }
        } finally {
            InstallResultReceiver.outcomes.remove(action)
        }
    }
}
