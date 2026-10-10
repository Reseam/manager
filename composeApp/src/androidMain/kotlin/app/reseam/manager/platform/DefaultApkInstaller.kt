package app.reseam.manager.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

class DefaultApkInstaller(private val context: Context) {
    suspend fun install(apk: File): ArtifactOutcome {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive = checkNotNull(context.packageManager.getPackageArchiveInfo(apk.path, flags)) {
            "The patched APK could not be read"
        }
        val signers = archive.signers()
        check(signers.isNotEmpty()) { "The patched APK has no signing certificates" }
        val installed = CompletableDeferred<Unit>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.data?.schemeSpecificPart != archive.packageName) return
                val current = runCatching { context.packageManager.getPackageInfo(archive.packageName, flags) }.getOrNull() ?: return
                if (PackageInfoCompat.getLongVersionCode(current) == PackageInfoCompat.getLongVersionCode(archive) &&
                    current.signers() == signers
                ) installed.complete(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.artifacts", apk)
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
            return if (withTimeoutOrNull(120_000L) { installed.await() } != null) {
                ArtifactOutcome.Installed(archive.packageName)
            } else {
                ArtifactOutcome.OpenedInstaller
            }
        } finally {
            context.unregisterReceiver(receiver)
        }
    }
}

private fun PackageInfo.signers(): Set<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    signingInfo?.apkContentsSigners.orEmpty().toSet()
} else {
    @Suppress("DEPRECATION")
    signatures.orEmpty().toSet()
}
