package app.reseam.manager

import app.reseam.manager.data.AppSource
import app.reseam.manager.data.AppliedPatch
import app.reseam.manager.data.BundleRef
import app.reseam.manager.data.BundleRepository
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.SettingsRepository
import app.reseam.manager.data.SigningKeyRepository
import app.reseam.manager.data.ref
import app.reseam.manager.platform.DeviceProfile
import app.reseam.manager.platform.InstalledApps
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.OptionValue
import app.reseam.sdk.PatchSelection
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class DebugReports(
    private val installedApps: InstalledApps?,
    private val actions: PatchedAppActions,
    private val bundles: BundleRepository,
    private val signingKeys: SigningKeyRepository,
    private val settings: SettingsRepository,
    private val device: DeviceProfile?,
) {
    @OptIn(ExperimentalTime::class)
    suspend fun of(app: PatchedApp): String {
        val installed = installedApps?.query(listOfNotNull(app.packageName, app.installedAs))?.associateBy { it.packageName }
        val settings = settings.settings.value
        return buildString {
            section("Reseam Manager")
            field("Version", ManagerVersion)
            field("Engine", EngineVersion)
            field("System", listOf("os.name", "os.version", "os.arch").joinToString(" ") { System.getProperty(it).orEmpty() })
            device?.let { field("Device", "${it.model}, API ${it.sdk}, ${it.abis.joinToString(" ")}, ${it.densityDpi} dpi") }

            section("App")
            field("Name", app.name)
            field("Package", app.packageName)
            field("Patched version", app.versionName)
            field("Patched at", Instant.fromEpochMilliseconds(app.patchedAtEpochMs))
            field("Source", app.source?.describe())
            field("Install method", app.installMethod.name.lowercase())
            field("Installed as", app.installedAs)
            installed?.let {
                field("Installed version", it[app.packageName]?.versionName ?: "not installed")
                field("Patched copy installed", (app.installedAs in it).yesNo)
            }
            if (app.installMethod == InstallMethod.MOUNT) field("Mounted", actions.isMounted(app.packageName).yesNo)
            field("Signing key", signingKeys.info.value?.fingerprint)

            section("Patches applied (${app.patches.size})")
            app.patches.forEach { item(it.describe()) }

            app.selection?.let { selection(it) }

            section("Patch sources used")
            app.bundles.forEach { item(it.describe()) }
            section("Patch sources now")
            bundles.installed().forEach { item(it.ref.describe()) }

            section("Settings")
            field("Update server", settings.serverUrl)
            field("Auto-update patches", settings.autoUpdateBundles.yesNo)
            field("Allow untested versions", settings.allowIncompatiblePatches.yesNo)
            field("Use the system installer", settings.useSystemInstaller.yesNo)
            field("Keep downloads", settings.keepDownloads.yesNo)
            field("Mount with root", settings.mountWithRoot.yesNo)
        }
    }
}

private fun StringBuilder.selection(selection: PatchSelection) {
    section("Selection")
    field("Preset", selection.preset.name.lowercase())
    field("Untested versions", selection.ignoreVersions.yesNo)
    list("Enabled", selection.enable)
    list("Disabled", selection.disable)
    list("Options", selection.options.flatMap { (patch, options) -> options.map { (key, value) -> "$patch.$key = ${value.describe()}" } })
}

private fun StringBuilder.section(title: String) {
    if (isNotEmpty()) append('\n')
    append(title).append('\n')
}

private fun StringBuilder.field(label: String, value: Any?) {
    append("  ").append(label).append(": ").append(value ?: "none").append('\n')
}

private fun StringBuilder.item(text: String) {
    append("  ").append(text).append('\n')
}

private fun StringBuilder.list(label: String, values: List<String>) {
    if (values.isEmpty()) return field(label, null)
    append("  ").append(label).append(":\n")
    values.forEach { append("    ").append(it).append('\n') }
}

private val Boolean.yesNo get() = if (this) "yes" else "no"

private fun AppSource.describe(): String = when (this) {
    is AppSource.Download -> "download"
    is AppSource.Installed -> "installed app"
    is AppSource.Saved -> "saved APK"
    is AppSource.File -> "file $path"
} + (versionName?.let { " $it" } ?: "")

private fun AppliedPatch.describe() = "$name ($bundle/$id)"

private fun BundleRef.describe() = "$name ${version ?: "unversioned"} ($id, $origin)"

private fun OptionValue.describe(): String = when (this) {
    is OptionValue.Text -> field0
    is OptionValue.Bool -> field0.yesNo
    is OptionValue.Int -> field0.toString()
    is OptionValue.Float -> field0.toString()
    is OptionValue.TextList -> field0.joinToString()
    is OptionValue.Path -> field0
}
