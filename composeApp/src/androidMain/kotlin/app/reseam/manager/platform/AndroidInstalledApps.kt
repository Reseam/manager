package app.reseam.manager.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidInstalledApps(private val context: Context) : InstalledApps {
    override suspend fun query(packageNames: Collection<String>): List<InstalledApp> = withContext(Dispatchers.IO) {
        packageNames.filter { it != context.packageName }.mapNotNull { packageInfo(it)?.toInstalledApp() }
    }

    override suspend fun all(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        apps.filter { it.packageName != context.packageName && it.isUserVisible() }
            .mapNotNull { packageInfo(it.packageName)?.toInstalledApp() }
    }

    override fun launch(packageName: String) {
        val intent = checkNotNull(context.packageManager.getLaunchIntentForPackage(packageName)) { "$packageName has no screen to open" }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun ApplicationInfo.isUserVisible(): Boolean =
        flags and ApplicationInfo.FLAG_SYSTEM == 0 || flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

    private fun packageInfo(packageName: String): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun PackageInfo.toInstalledApp(): InstalledApp? {
        val app = applicationInfo ?: return null
        return InstalledApp(
            packageName = packageName,
            name = context.packageManager.getApplicationLabel(app).toString(),
            versionName = versionName,
            apkPath = app.sourceDir,
            splitPaths = app.splitSourceDirs?.toList().orEmpty(),
        )
    }
}
