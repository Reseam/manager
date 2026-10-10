package app.reseam.manager.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.reseam.manager.data.Failure
import app.reseam.manager.data.exportArtifact
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AndroidInstaller(private val context: Context) : ArtifactAction {
    override val kind = ArtifactAction.Kind.Install

    override suspend fun run(artifact: PlatformFile, useSystemInstaller: Boolean): ArtifactOutcome {
        if (!artifact.exists()) throw Failure.OutputMissing()
        if (useSystemInstaller && !artifact.isDirectory()) {
            return DefaultApkInstaller(context).install(File(artifact.path))
        }
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
        if (apks.isEmpty()) throw Failure.OutputMissing()
        return install(apks)
    }

    /** A split set goes out as one `.apks` file, which split-aware installers open. */
    override suspend fun share(artifact: PlatformFile): Boolean {
        if (!artifact.exists()) throw Failure.OutputMissing()
        val file = if (artifact.isDirectory()) {
            val shared = File(context.cacheDir, "share").apply { mkdirs() }.resolve("${artifact.name}.apks")
            exportArtifact(artifact, PlatformFile(shared))
            shared
        } else {
            File(artifact.path)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.artifacts", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/vnd.android.package-archive")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }

    override suspend fun uninstall(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val result = awaitResult { context.packageManager.packageInstaller.uninstall(packageName, it) }
        result?.status == PackageInstaller.STATUS_SUCCESS
    }

    /** Runs [start] with a status receiver and waits for the system to report back, null when it never does. */
    private suspend fun awaitResult(start: (IntentSender) -> Unit): InstallResult? {
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
            start(pending.intentSender)
            return withTimeoutOrNull(120_000L) { outcome.await() }
        } finally {
            InstallResultReceiver.outcomes.remove(action)
        }
    }

    /** A session opens the system confirmation directly, where a view intent can land on an OEM store's chooser. */
    private suspend fun install(apks: List<File>): ArtifactOutcome = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        // Honored only when Manager updates itself or an app it installed; every other install still asks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        val result = awaitResult { sender ->
            val session = installer.openSession(installer.createSession(params))
            try {
                apks.forEach { apk ->
                    session.openWrite(apk.name, 0, apk.length()).use { stream ->
                        FileInputStream(apk).use { it.copyTo(stream) }
                        session.fsync(stream)
                    }
                }
                session.commit(sender)
            } catch (error: Exception) {
                runCatching { session.abandon() }
                throw error
            } finally {
                runCatching { session.close() }
            }
        } ?: return@withContext ArtifactOutcome.TimedOut
        when (result.status) {
            PackageInstaller.STATUS_SUCCESS -> ArtifactOutcome.Installed(checkNotNull(result.packageName) { "The installer reported success without a package name" })
            PackageInstaller.STATUS_FAILURE_ABORTED -> ArtifactOutcome.Cancelled
            else -> ArtifactOutcome.Failed(
                reason = when (result.status) {
                    PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallFailure.Conflict
                    PackageInstaller.STATUS_FAILURE_STORAGE -> InstallFailure.Storage
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallFailure.Incompatible
                    PackageInstaller.STATUS_FAILURE_INVALID -> InstallFailure.Invalid
                    PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallFailure.Blocked
                    else -> InstallFailure.Other
                },
                detail = result.message,
                packageName = context.packageManager.getPackageArchiveInfo(apks.first().path, 0)?.packageName,
            )
        }
    }
}
