package app.reseam.manager.platform

data class InstalledApp(
    val packageName: String,
    val name: String,
    val versionName: String?,
    val apkPath: String,
    val splitPaths: List<String>,
)

interface InstalledApps {
    suspend fun query(packageNames: Collection<String>): List<InstalledApp>

    suspend fun all(): List<InstalledApp>

    fun launch(packageName: String)
}
