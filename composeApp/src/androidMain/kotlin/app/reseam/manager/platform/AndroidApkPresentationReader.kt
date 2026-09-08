package app.reseam.manager.platform

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import app.reseam.manager.sdk.ApkArchive
import java.io.ByteArrayOutputStream

private const val IconSize = 192

/** The package manager reads the archive like an installed app: localized label, adaptive and vector icons rendered. */
class AndroidApkPresentationReader(private val context: Context) : ApkPresentationReader {
    override fun read(archive: ApkArchive): AppPresentation {
        val packageManager = context.packageManager
        val info = packageManager.getPackageArchiveInfo(archive.basePath, 0)?.applicationInfo
            ?: return AppPresentation(archive.metadata.applicationLabel, null)
        info.sourceDir = archive.basePath
        info.publicSourceDir = archive.basePath
        info.splitSourceDirs = archive.splitPaths.toTypedArray()
        info.splitPublicSourceDirs = info.splitSourceDirs
        val resources = packageManager.getResourcesForApplication(info)
        val label = info.nonLocalizedLabel?.toString()
            ?: info.labelRes.takeIf { it != 0 }?.let { resources.getText(it).toString() }
            ?: archive.metadata.applicationLabel
        val icon = info.icon.takeIf { it != 0 }?.let { resource ->
            val bitmap = resources.getDrawable(resource, null).toBitmap(IconSize, IconSize, Bitmap.Config.ARGB_8888)
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode the app icon" }
                output.toByteArray()
            }
        }
        return AppPresentation(label, icon)
    }
}
