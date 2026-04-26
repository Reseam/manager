package app.reseam.manager.data.repository

import app.cash.sqldelight.TransactionWithoutReturn
import app.reseam.manager.data.db.ReseamDatabase
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.domain.repository.PatchedAppStore
import app.reseam.manager.domain.repository.SettingsStore
import app.reseam.manager.patcher.CompatibilityMetadata
import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.patcher.OptionKind
import app.reseam.manager.patcher.OptionMetadata
import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.patcher.ReseamJson
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.PatchUpdateInfo
import app.reseam.manager.ui.model.PatchedAppSummary
import app.reseam.manager.ui.model.SettingsState
import app.reseam.manager.ui.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

private fun patchId(bundleId: String, patchName: String): String = "$bundleId:$patchName"

class SqlBundleStore(private val db: ReseamDatabase) : BundleStore {
    override suspend fun list(): List<BundleSummary> = withContext(Dispatchers.Default) {
        db.bundlesQueries.selectAll().executeAsList().map {
            BundleSummary(
                id = it.id,
                name = it.name,
                description = it.description,
                source = it.source,
                official = it.official != 0L,
                trusted = it.trusted != 0L,
                patchCount = it.patch_count.toInt(),
                version = it.version,
                updatedLabel = it.updated_label,
                signerPublicKeyHex = it.signer_public_key_hex,
                signerFingerprint = it.signer_fingerprint,
                path = it.path,
            )
        }
    }

    override suspend fun save(bundle: BundleSummary): Unit = withContext(Dispatchers.Default) {
        db.bundlesQueries.upsert(
            id = bundle.id,
            name = bundle.name,
            description = bundle.description,
            source = bundle.source,
            official = if (bundle.official) 1L else 0L,
            trusted = if (bundle.trusted) 1L else 0L,
            patch_count = bundle.patchCount.toLong(),
            version = bundle.version,
            updated_label = bundle.updatedLabel,
            signer_public_key_hex = bundle.signerPublicKeyHex,
            signer_fingerprint = bundle.signerFingerprint,
            path = bundle.path,
        )
    }

    override suspend fun remove(bundleId: String): Unit = withContext(Dispatchers.Default) {
        val existing = db.bundlesQueries.selectById(bundleId).executeAsOneOrNull() ?: return@withContext
        if (existing.official != 0L) return@withContext
        db.bundlesQueries.deleteById(bundleId)
    }
}

class SqlPatchStore(private val db: ReseamDatabase) : PatchStore {
    override suspend fun listForBundle(bundleId: String): List<PatchMetadata> = withContext(Dispatchers.Default) {
        val bundleName = db.bundlesQueries.selectById(bundleId).executeAsOneOrNull()?.name ?: bundleId
        db.patchesQueries.selectByBundle(bundleId).executeAsList().map { row ->
            hydratePatch(row.id, bundleName, row.name, row.description, row.enabled_by_default != 0L)
        }
    }

    override suspend fun listByPackage(packageName: String): List<PatchMetadata> = withContext(Dispatchers.Default) {
        val bundleNamesById = db.bundlesQueries.selectAll().executeAsList().associate { it.id to it.name }
        db.patchesQueries.selectByPackageAndTrusted(packageName).executeAsList().map { row ->
            val bundleName = bundleNamesById[row.bundle_id] ?: row.bundle_id
            hydratePatch(row.id, bundleName, row.name, row.description, row.enabled_by_default != 0L)
        }
    }

    override suspend fun compatibleCountByPackage(): Map<String, Int> = withContext(Dispatchers.Default) {
        db.patchesQueries.countCompatibleByPackage().executeAsList().associate {
            it.package_name to it.patch_count.toInt()
        }
    }

    override suspend fun replaceForBundle(bundleId: String, patches: List<PatchMetadata>): Unit = withContext(Dispatchers.Default) {
        db.transaction {
            replaceForBundleInTx(bundleId, patches)
        }
    }

    override suspend fun deleteForBundle(bundleId: String): Unit = withContext(Dispatchers.Default) {
        db.patchesQueries.deleteByBundle(bundleId)
    }

    private fun TransactionWithoutReturn.replaceForBundleInTx(bundleId: String, patches: List<PatchMetadata>) {
        db.patchesQueries.deleteByBundle(bundleId)
        patches.forEach { patch ->
            val pid = patchId(bundleId, patch.name)
            db.patchesQueries.upsert(
                id = pid,
                bundle_id = bundleId,
                name = patch.name,
                description = patch.description,
                enabled_by_default = if (patch.enabledByDefault) 1L else 0L,
            )
            patch.dependencies.forEach { dep ->
                db.patchDependenciesQueries.upsert(patch_id = pid, dep_name = dep)
            }
            patch.compatibleWith.forEach { compat ->
                db.patchCompatibilityQueries.upsert(
                    patch_id = pid,
                    package_name = compat.packageName,
                    versions_json = ReseamJson.codec.encodeToString(compat.versions),
                )
            }
            patch.options.forEachIndexed { idx, opt ->
                db.patchOptionsQueries.upsert(
                    patch_id = pid,
                    ord = idx.toLong(),
                    key = opt.key,
                    title = opt.title,
                    description = opt.description,
                    option_type = optionKindToString(opt.optionType),
                    default_value_json = opt.defaultValue?.let { ReseamJson.codec.encodeToString<InputOptionValue>(it) },
                    valid_values_json = opt.validValues?.let { ReseamJson.codec.encodeToString(it) },
                    required = if (opt.required) 1L else 0L,
                )
            }
        }
    }

    private fun hydratePatch(
        pid: String,
        bundleName: String,
        name: String,
        description: String,
        enabledByDefault: Boolean,
    ): PatchMetadata {
        val deps = db.patchDependenciesQueries.selectByPatch(pid).executeAsList()
        val compat = db.patchCompatibilityQueries.selectByPatch(pid).executeAsList().map { c ->
            CompatibilityMetadata(
                packageName = c.package_name,
                versions = runCatching {
                    ReseamJson.codec.decodeFromString<List<String>>(c.versions_json)
                }.getOrDefault(emptyList()),
            )
        }
        val opts = db.patchOptionsQueries.selectByPatch(pid).executeAsList().map { o ->
            OptionMetadata(
                key = o.key,
                title = o.title,
                description = o.description,
                optionType = optionKindFromString(o.option_type),
                defaultValue = o.default_value_json?.let {
                    runCatching { ReseamJson.codec.decodeFromString<InputOptionValue>(it) }.getOrNull()
                },
                validValues = o.valid_values_json?.let {
                    runCatching { ReseamJson.codec.decodeFromString<List<String>>(it) }.getOrNull()
                },
                required = o.required != 0L,
            )
        }
        return PatchMetadata(
            sourceBundle = bundleName,
            name = name,
            description = description,
            enabledByDefault = enabledByDefault,
            dependencies = deps,
            compatibleWith = compat,
            options = opts,
            isCompatible = false,
            incompatibilityReason = null,
        )
    }
}

private fun optionKindToString(kind: OptionKind): String = when (kind) {
    OptionKind.String -> "string"
    OptionKind.Bool -> "bool"
    OptionKind.Int -> "int"
    OptionKind.Float -> "float"
    OptionKind.StringList -> "string_list"
    OptionKind.Path -> "path"
}

private fun optionKindFromString(value: String): OptionKind = when (value) {
    "string" -> OptionKind.String
    "bool" -> OptionKind.Bool
    "int" -> OptionKind.Int
    "float" -> OptionKind.Float
    "string_list" -> OptionKind.StringList
    "path" -> OptionKind.Path
    else -> OptionKind.String
}

class SqlPatchedAppStore(private val db: ReseamDatabase) : PatchedAppStore {
    override suspend fun list(): List<PatchedAppSummary> = withContext(Dispatchers.Default) {
        db.patchedAppsQueries.selectAll().executeAsList().map { row ->
            val bundleNames = db.patchedAppBundlesQueries.selectByApp(row.id).executeAsList()
            PatchedAppSummary(
                id = row.id,
                name = row.name,
                packageName = row.package_name,
                versionName = row.version_name,
                patchCount = row.patch_count.toInt(),
                artifactPath = row.artifact_path,
                bundleNames = bundleNames,
                update = row.update_version_name?.let { v ->
                    PatchUpdateInfo(versionName = v, compatible = (row.update_compatible ?: 0L) != 0L)
                },
            )
        }
    }

    override suspend fun save(app: PatchedAppSummary): Unit = withContext(Dispatchers.Default) {
        db.transaction {
            db.patchedAppsQueries.upsert(
                id = app.id,
                name = app.name,
                package_name = app.packageName,
                version_name = app.versionName,
                patch_count = app.patchCount.toLong(),
                artifact_path = app.artifactPath,
                update_version_name = app.update?.versionName,
                update_compatible = app.update?.let { if (it.compatible) 1L else 0L },
            )
            db.patchedAppBundlesQueries.deleteByApp(app.id)
            app.bundleNames.forEachIndexed { idx, name ->
                db.patchedAppBundlesQueries.insert(
                    patched_app_id = app.id,
                    ord = idx.toLong(),
                    bundle_name = name,
                )
            }
        }
    }
}

class SqlSettingsStore(private val db: ReseamDatabase) : SettingsStore {
    override suspend fun load(): SettingsState = withContext(Dispatchers.Default) {
        val row = db.settingsQueries.select().executeAsOneOrNull()
        if (row == null) {
            SettingsState()
        } else {
            SettingsState(
                checkUpdatesDaily = row.check_updates_daily != 0L,
                analyticsEnabled = row.analytics_enabled != 0L,
                theme = themeFromString(row.theme),
                onboardingCompleted = row.onboarding_completed != 0L,
                apiBaseUrl = row.api_base_url,
            )
        }
    }

    override suspend fun save(settings: SettingsState): Unit = withContext(Dispatchers.Default) {
        db.settingsQueries.upsert(
            check_updates_daily = if (settings.checkUpdatesDaily) 1L else 0L,
            analytics_enabled = if (settings.analyticsEnabled) 1L else 0L,
            theme = settings.theme.name,
            onboarding_completed = if (settings.onboardingCompleted) 1L else 0L,
            api_base_url = settings.apiBaseUrl,
        )
    }
}

private fun themeFromString(value: String): ThemeMode =
    runCatching { ThemeMode.valueOf(value) }.getOrDefault(ThemeMode.System)
