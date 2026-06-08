package app.reseam.manager.data.platform

import android.content.Context
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
    override suspend fun apps(packageNames: Collection<String>): List<InstalledAppSummary> {
        if (packageNames.isEmpty()) return emptyList()
        val excluded = context.packageName
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            packageNames.asSequence()
                .filter { it != excluded }
                .mapNotNull { pkg -> pm.packageInfo(pkg)?.toSummary(pm) }
                .toList()
        }
    }

    private fun PackageInfo.toSummary(pm: PackageManager): InstalledAppSummary? {
        val app = applicationInfo ?: return null
        val sourceDir = app.sourceDir ?: return null
        val splits = app.splitSourceDirs?.toList().orEmpty()
        val paths = listOf(sourceDir) + splits
        return InstalledAppSummary(
            id = app.packageName,
            name = pm.getApplicationLabel(app).toString(),
            packageName = app.packageName,
            versionName = versionName,
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
