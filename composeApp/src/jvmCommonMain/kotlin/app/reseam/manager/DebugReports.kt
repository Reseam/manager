package app.reseam.manager

import app.reseam.manager.data.BundleRef
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.Settings
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.SigningKeyRepository
import app.reseam.manager.data.ref
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.InstalledApps
import app.reseam.sdk.InstallMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DebugReport(
    val manager: String,
    val engine: String,
    val system: String,
    val device: DeviceProfile?,
    val app: PatchedApp,
    val installedVersion: String?,
    val patchedInstalled: Boolean?,
    val mounted: Boolean?,
    val signingKey: String?,
    val sources: List<BundleRef>,
    val settings: Settings,
)

private val ReportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

class DebugReports(
    private val installedApps: InstalledApps?,
    private val actions: PatchedAppActions,
    private val bundles: BundleRepository,
    private val signingKeys: SigningKeyRepository,
    private val settings: SettingsRepository,
    private val device: DeviceProfile?,
) {
    suspend fun of(app: PatchedApp): String {
        val installed = installedApps?.query(listOfNotNull(app.packageName, app.installedAs))?.associateBy { it.packageName }
        val report = DebugReport(
            manager = ManagerVersion,
            engine = EngineVersion,
            system = listOf("os.name", "os.version", "os.arch").joinToString(" ") { System.getProperty(it).orEmpty() },
            device = device,
            app = app,
            installedVersion = installed?.get(app.packageName)?.versionName,
            patchedInstalled = installed?.let { app.installedAs in it },
            mounted = if (app.installMethod == InstallMethod.MOUNT) actions.isMounted(app.packageName) else null,
            signingKey = signingKeys.info.value?.fingerprint,
            sources = bundles.installed().map { it.ref },
            settings = settings.settings.value,
        )
        return ReportJson.encodeToString(report)
    }
}
