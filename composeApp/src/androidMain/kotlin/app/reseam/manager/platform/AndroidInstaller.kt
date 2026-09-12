package app.reseam.manager.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
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

class AndroidInstaller(
    private val context: Context,
    private val onResult: (String) -> Unit = {},
) : ArtifactAction {
    override val label = "Install"

    override suspend fun run(artifact: PlatformFile) {
        check(artifact.exists()) { "The patched APK is missing: ${artifact.path}" }
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        if (artifact.isDirectory()) {
            installSplitSet(artifact)
        } else {
            launchSystemInstaller(artifact)
        }
    }

    private fun launchSystemInstaller(apk: PlatformFile) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(apk.path))
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private suspend fun installSplitSet(dir: PlatformFile) = withContext(Dispatchers.IO) {
        val apks = dir.list().map { File(it.path) }.filter { it.extension.equals("apk", ignoreCase = true) }
        check(apks.isNotEmpty()) { "The patched split set has no APKs: ${dir.path}" }

        val action = "app.reseam.manager.install.${System.nanoTime()}"
        val outcome = CompletableDeferred<Pair<Int, String?>>()
        InstallResultReceiver.outcomes[action] = outcome
        try {
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, InstallResultReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            val installer = context.packageManager.packageInstaller
            val sessionId = installer.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
            val session = installer.openSession(sessionId)
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
            val (status, message) = withTimeoutOrNull(120_000L) { outcome.await() }
                ?: return@withContext onResult("The install timed out")
            onResult(
                when (status) {
                    PackageInstaller.STATUS_SUCCESS -> "Patched app installed"
                    else -> "Install failed: ${message ?: "status $status"}"
                },
            )
        } finally {
            InstallResultReceiver.outcomes.remove(action)
        }
    }
}
