package app.reseam.manager.platform

data class InstalledApp(
    val packageName: String,
    val name: String,
    val versionName: String?,
    val apkPath: String,
    val splitPaths: List<String>,
)

/** Apps installed on this device. Absent on platforms without a package manager. */
interface InstalledApps {
    suspend fun query(packageNames: Collection<String>): List<InstalledApp>
}
