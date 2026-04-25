package app.reseam.manager.data.repository

import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.data.platform.desktopDataFile
import app.reseam.manager.patcher.ReseamJson
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.model.SettingsState
import kotlinx.serialization.encodeToString
import java.io.File

class DesktopPatchedAppStore(
    private val file: File = desktopDataFile("patched-apps.json"),
) : PatchedAppStore {
    override suspend fun list(): List<PatchedAppSummary> = file.readJsonList()
    override suspend fun save(app: PatchedAppSummary) = file.writeJsonList(list().filterNot { it.id == app.id } + app)
}

class DesktopBundleStore(
    private val file: File = desktopDataFile("bundles.json"),
) : BundleStore {
    override suspend fun list(): List<BundleSummary> = file.readJsonList<BundleSummary>().ifEmpty { listOf(BundleSummary.official()) }
    override suspend fun save(bundle: BundleSummary) = file.writeJsonList(list().filterNot { it.id == bundle.id } + bundle)
    override suspend fun remove(bundleId: String) = file.writeJsonList(list().filter { it.official || it.id != bundleId })
}

class DesktopSettingsStore(
    private val file: File = desktopDataFile("settings.json"),
) : SettingsStore {
    override suspend fun load(): SettingsState =
        try {
            if (!file.isFile) SettingsState() else ReseamJson.codec.decodeFromString<SettingsState>(file.readText())
        } catch (_: Exception) {
            SettingsState()
        }

    override suspend fun save(settings: SettingsState) {
        file.parentFile?.mkdirs()
        file.writeText(ReseamJson.codec.encodeToString(settings))
    }
}

internal inline fun <reified T> File.readJsonList(): List<T> =
    try {
        if (!isFile) emptyList() else ReseamJson.codec.decodeFromString<List<T>>(readText())
    } catch (_: Exception) {
        emptyList()
    }

internal inline fun <reified T> File.writeJsonList(values: List<T>) {
    parentFile?.mkdirs()
    writeText(ReseamJson.codec.encodeToString(values))
}
