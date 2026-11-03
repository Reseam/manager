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
import app.reseam.sdk.dexMetadata
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val PlayProtectConsent = "package_verifier_user_consent"

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

    /**
     * Before Android 12 every install asks. After, Play Protect still asks about every patched build, since it can't
     * have seen one before, unless its app scanning is off: an undocumented global, -1 when off. Anything else counts as on.
     */
    override val asksBeforeUpdates: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || Settings.Global.getInt(context.contentResolver, PlayProtectConsent, 0) != -1

    /** Android 14 is the first that says; before it, every app counts as closed. */
    override suspend fun inUse(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        val result = try {
            suspendCancellableCoroutine<PackageInstaller.InstallConstraintsResult> { continuation ->
                context.packageManager.packageInstaller.checkInstallConstraints(
                    listOf(packageName),
                    PackageInstaller.InstallConstraints.GENTLE_UPDATE,
                    context.mainExecutor,
                ) { continuation.resume(it) }
            }
        } catch (_: SecurityException) {
            // Only the installer of record may ask; another installer put this app on the device.
            return false
        }
        return !result.areAllConstraintsSatisfied()
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
        when (result.status) {
            PackageInstaller.STATUS_SUCCESS -> ArtifactOutcome.Installed(checkNotNull(result.packageName) { "The installer reported success without a package name" })
            PackageInstaller.STATUS_FAILURE_ABORTED -> ArtifactOutcome.Cancelled
            else -> {
                val archive = context.packageManager.getPackageArchiveInfo(apks.first().path, 0)
                ArtifactOutcome.Failed(
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
                    packageName = archive?.packageName,
                )
            }
        }
    }

    private suspend fun commit(apks: List<File>, userAction: Int): InstallResult? {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        // Honored only when Manager updates itself or an app it installed; every other install still asks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.setRequireUserAction(userAction)
        return awaitResult { sender ->
            val session = installer.openSession(installer.createSession(params))
            try {
                apks.forEach { apk ->
                    session.openWrite(apk.name, 0, apk.length()).use { stream ->
                        FileInputStream(apk).use { it.copyTo(stream) }
                        session.fsync(stream)
                    }
                    // Without it ART runs the app uncompiled until the device's idle maintenance.
                    dexMetadata(apk.path, Build.VERSION.SDK_INT.toUInt())?.let { metadata ->
                        session.openWrite("${apk.nameWithoutExtension}.dm", 0, metadata.size.toLong()).use { stream ->
                            stream.write(metadata)
                            session.fsync(stream)
                        }
                    }
                }
                session.commit(sender)
            } catch (error: Exception) {
                runCatching { session.abandon() }
                throw error
            } finally {
                runCatching { session.close() }
            }
        }
    }
}
