package app.reseam.manager.data.repository

import android.content.Context
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.patcher.ReseamJson
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.model.SettingsState
import kotlinx.serialization.encodeToString
import java.io.File

class AndroidPatchedAppStore(
    private val file: File,
) : PatchedAppStore {
    constructor(context: Context) : this(context.reseamDataFile("patched-apps.json"))

    override suspend fun list(): List<PatchedAppSummary> = file.readJsonList()
    override suspend fun save(app: PatchedAppSummary) = file.writeJsonList(list().filterNot { it.id == app.id } + app)
}

class AndroidBundleStore(
    private val file: File,
) : BundleStore {
    constructor(context: Context) : this(context.reseamDataFile("bundles.json"))

    override suspend fun list(): List<BundleSummary> = file.readJsonList<BundleSummary>().ifEmpty { listOf(BundleSummary.official()) }
    override suspend fun save(bundle: BundleSummary) = file.writeJsonList(list().filterNot { it.id == bundle.id } + bundle)
    override suspend fun remove(bundleId: String) = file.writeJsonList(list().filter { it.official || it.id != bundleId })
}

class AndroidSettingsStore(
    private val file: File,
) : SettingsStore {
    constructor(context: Context) : this(context.reseamDataFile("settings.json"))

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

internal fun Context.reseamDataFile(name: String): File =
    File(filesDir, "reseam/$name")
