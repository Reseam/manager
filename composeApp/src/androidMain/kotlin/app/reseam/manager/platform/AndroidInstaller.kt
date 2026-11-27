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
import app.reseam.manager.data.Installer
import app.reseam.manager.data.exportArtifact
import app.reseam.sdk.writeApks
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val PlayProtectConsent = "package_verifier_user_consent"

class AndroidInstaller(private val context: Context) : ArtifactAction {
    override val kind = ArtifactAction.Kind.Install

    /** The sessions this process waits on; any other session of Manager's is one an earlier wait left behind. */
    private val waiting = mutableSetOf<Int>()

    override suspend fun run(artifact: PlatformFile, installer: Installer): ArtifactOutcome {
        if (!artifact.exists()) throw Failure.OutputMissing()
        if (installer == Installer.OtherApp && !artifact.isDirectory()) {
            return DefaultApkInstaller(context).install(File(artifact.path))
        }
        val apks = if (artifact.isDirectory()) {
            artifact.list().map { File(it.path) }.filter { it.extension.equals("apk", ignoreCase = true) }
        } else {
            listOf(File(artifact.path))
        }
        if (apks.isEmpty()) throw Failure.OutputMissing()
        return when {
            installer == Installer.Shizuku -> installThroughShizuku(apks)
            context.packageManager.canRequestPackageInstalls() -> install(apks)
            else -> {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                ArtifactOutcome.PermissionRequested
            }
        }
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

    override suspend fun uninstall(packageName: String, installer: Installer): Boolean = withContext(Dispatchers.IO) {
        val packageInstaller = if (installer == Installer.Shizuku) ShizukuPackageInstaller.connect().installer else context.packageManager.packageInstaller
        awaitResult { packageInstaller.uninstall(packageName, it) }?.status == PackageInstaller.STATUS_SUCCESS
    }

    /**
     * Android's own prompt asks before every install until Android 12. After, Play Protect still asks about every patched
     * build, since it can't have seen one before, unless its app scanning is off: an undocumented global, -1 when off.
     * Anything else counts as on.
     */
    override fun asksBeforeUpdates(installer: Installer): Boolean = when (installer) {
        Installer.Shizuku -> false
        Installer.OtherApp -> true
        Installer.Android -> Build.VERSION.SDK_INT < Build.VERSION_CODES.S || Settings.Global.getInt(context.contentResolver, PlayProtectConsent, 0) != -1
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
            InstallResultReceiver.prompted.remove(action)
        }
    }

    /** A session opens the system confirmation directly, where a view intent can land on an OEM store's chooser. */
    private suspend fun install(apks: List<File>): ArtifactOutcome = withContext(Dispatchers.IO) {
        val silent = commit(apks, PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED) ?: return@withContext ArtifactOutcome.TimedOut
        // HyperOS aborts a silent update instead of asking.
        val result = if (silent.status == PackageInstaller.STATUS_FAILURE_ABORTED && !silent.prompted) {
            commit(apks, PackageInstaller.SessionParams.USER_ACTION_REQUIRED) ?: return@withContext ArtifactOutcome.TimedOut
        } else {
            silent
        }
        outcome(result, apks)
    }

    /** Sessions through Shizuku belong to the shell, whose other sessions may be an adb install, so none are swept. */
    private suspend fun installThroughShizuku(apks: List<File>): ArtifactOutcome = withContext(Dispatchers.IO) {
        val shizuku = ShizukuPackageInstaller.connect()
        val id = shizuku.createSession()
        try {
            write(id, apks, shizuku::openSession)?.let { outcome(it, apks) } ?: ArtifactOutcome.TimedOut
        } finally {
            abandonOpen(shizuku.installer, id)
        }
    }

    private fun outcome(result: InstallResult, apks: List<File>): ArtifactOutcome = when (result.status) {
        PackageInstaller.STATUS_SUCCESS -> ArtifactOutcome.Installed(checkNotNull(result.packageName) { "The installer reported success without a package name" })
        PackageInstaller.STATUS_FAILURE_ABORTED -> ArtifactOutcome.Cancelled
        else -> ArtifactOutcome.Failed(
            reason = when (result.status) {
                PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallFailure.Conflict
                PackageInstaller.STATUS_FAILURE_STORAGE -> InstallFailure.Storage
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallFailure.Incompatible
                // Android reports a downgrade under the same status as a broken APK.
                PackageInstaller.STATUS_FAILURE_INVALID -> if (result.legacyStatus == InstallFailedVersionDowngrade) InstallFailure.Downgrade else InstallFailure.Invalid
                PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallFailure.Blocked
                else -> InstallFailure.Other
            },
            detail = result.message,
            packageName = context.packageManager.getPackageArchiveInfo(apks.first().path, 0)?.packageName,
        )
    }

    /**
     * A session that never reaches a result stays open until abandoned, and Android refuses new sessions once an app holds
     * too many. So one the wait gives up on is abandoned, and so is any left open by an earlier run before a new one starts.
     */
    private suspend fun commit(apks: List<File>, userAction: Int): InstallResult? {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        // Honored only when Manager updates itself or an app it installed; every other install still asks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.setRequireUserAction(userAction)
        val id = synchronized(waiting) {
            installer.mySessions.filter { it.sessionId !in waiting }.forEach { abandon(installer, it.sessionId) }
            installer.createSession(params).also { waiting += it }
        }
        try {
            return write(id, apks, installer::openSession)
        } finally {
            synchronized(waiting) { waiting -= id }
            abandonOpen(installer, id)
        }
    }

    private suspend fun write(id: Int, apks: List<File>, open: (Int) -> PackageInstaller.Session): InstallResult? = awaitResult { sender ->
        val session = open(id)
        try {
            session.writeApks(apks)
            session.commit(sender)
        } finally {
            runCatching { session.close() }
        }
    }

    private fun abandonOpen(installer: PackageInstaller, id: Int) {
        if (installer.getSessionInfo(id) != null) abandon(installer, id)
    }

    /** The session can finish on its own between the check and this call, which then has nothing to abandon. */
    private fun abandon(installer: PackageInstaller, id: Int) {
        runCatching { installer.abandonSession(id) }
    }
}
