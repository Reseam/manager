package app.reseam.manager.data.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import app.reseam.manager.domain.sources.InstalledAppSource
import app.reseam.manager.ui.model.InstalledAppSummary
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidInstalledAppSource(
    private val context: Context,
) : InstalledAppSource {
    override suspend fun installedApps(): List<InstalledAppSummary> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { it.sourceDir != null }
            .mapNotNull { appInfo -> appInfo.toSummary(packageManager) }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    private fun ApplicationInfo.toSummary(packageManager: PackageManager): InstalledAppSummary? {
        val packageInfo = packageManager.packageInfo(packageName) ?: return null
        val splits = splitSourceDirs?.toList().orEmpty()
        val paths = listOfNotNull(sourceDir) + splits
        return InstalledAppSummary(
            id = packageName,
            name = packageManager.getApplicationLabel(this).toString(),
            packageName = packageName,
            versionName = packageInfo.versionName,
            sizeBytes = paths.sumOf { File(it).length() },
            apkPath = sourceDir,
            splitPaths = splits,
            compatiblePatchCount = null,
        )
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.packageInfo(packageName: String): PackageInfo? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                getPackageInfo(packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
}
