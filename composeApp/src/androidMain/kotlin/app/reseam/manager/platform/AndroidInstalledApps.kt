package app.reseam.manager.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidInstalledApps(private val context: Context) : InstalledApps {
    override suspend fun query(packageNames: Collection<String>): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        packageNames.filter { it != context.packageName }.mapNotNull { packageName ->
            val info = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, 0)
                }
            } catch (_: PackageManager.NameNotFoundException) {
                return@mapNotNull null
            }
            val app = info.applicationInfo ?: return@mapNotNull null
            InstalledApp(
                packageName = packageName,
                name = pm.getApplicationLabel(app).toString(),
                versionName = info.versionName,
                apkPath = app.sourceDir,
                splitPaths = app.splitSourceDirs?.toList().orEmpty(),
            )
        }
    }
}
