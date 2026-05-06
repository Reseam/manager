package app.reseam.manager.data.platform

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import app.reseam.manager.domain.installer.PatchedAppInstaller
import app.reseam.manager.patcher.PatchArtifact
import java.io.File

class AndroidPatchedAppInstaller(
    private val context: Context,
) : PatchedAppInstaller {
    override suspend fun install(artifact: PatchArtifact) {
        val apk = File(artifact.path)
        check(apk.isFile) { "Patched APK does not exist: ${artifact.path}" }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk,
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }
}
