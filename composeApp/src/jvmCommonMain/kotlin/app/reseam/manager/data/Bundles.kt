package app.reseam.manager.data

import app.reseam.manager.platform.httpDownload
import app.reseam.manager.sdk.ReseamSdk
import app.reseam.sdk.BundleMetadata
import app.reseam.sdk.InspectRequest
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.Problem
import app.reseam.sdk.SdkError
import app.reseam.sdk.Trust
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.copyTo
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** What Manager records about an installed bundle. Its patches are read from the file at [path], so they always match the running engine. */
@Serializable
data class Bundle(
    /** The signer's public key, hex. One installed bundle per signer. */
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String?,
    val official: Boolean,
    val origin: String,
    val path: String,
    /** The `patches.json` this bundle updates from. The official bundle follows the API instead. */
    val index: String? = null,
    val checkedAtEpochMs: Long? = null,
) {
    val followsUpdates: Boolean get() = official || index != null
}

@Serializable
data class BundleLibrary(val bundles: List<Bundle> = emptyList())

sealed interface UpdateSource {
    val version: String

    data class Official(override val version: String) : UpdateSource

    data class Index(val url: String, override val version: String) : UpdateSource
}

/** A bundle file inspected but not yet installed. Patches are empty while [prompt] is set: untrusted code is never loaded. */
class StagedBundle internal constructor(
    val metadata: BundleMetadata,
    val origin: String,
    val prompt: TrustPrompt?,
    internal val file: PlatformFile,
    val patches: List<PatchMetadata>,
    val source: UpdateSource?,
)

enum class SyncScope { All, Due, None }

data class SyncFailure(val bundle: String, val error: Exception)

data class BundleSync(val prompt: StagedBundle?, val failures: List<SyncFailure>)

val UpdateInterval = 8.hours

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
private const val StagingPrefix = ".staging-"

private val ZipMagic = byteArrayOf(0x50, 0x4b, 0x03, 0x04)

class BundleRepository(
    private val store: JsonStore<BundleLibrary>,
    private val directory: PlatformFile,
) {
    val library: StateFlow<BundleLibrary> = store.state
    val bundles: Flow<List<Bundle>> = library.map { it.bundles }
    val syncing: StateFlow<Boolean> get() = syncingState

    /** The patches each installed bundle declares, by bundle id. Null until [load] has read the bundle files. */
    val patches: StateFlow<Map<String, List<PatchMetadata>>?> get() = patchesState

    /** The one staged bundle waiting for the user's trust decision, from any flow. */
    val pending: StateFlow<StagedBundle?> get() = pendingState

    private val syncingState = MutableStateFlow(false)
    private val pendingState = MutableStateFlow<StagedBundle?>(null)
    private val patchesState = MutableStateFlow<Map<String, List<PatchMetadata>>?>(null)
    private val syncLock = Mutex()

    /** Reads every installed bundle file. Bundles the running engine cannot use are uninstalled and returned with the reason, except a too-old bundle with an index, which the next sync replaces. */
    suspend fun load(): List<Pair<Bundle, Problem>> {
        val read = installed().map { bundle ->
            val response = ReseamSdk.inspect(InspectRequest(splitPaths = emptyList(), bundlePaths = listOf(bundle.path), trust = Trust(keys = listOf(bundle.id))))
            Triple(bundle, response.bundles.single().problem, response.patches)
        }
        val (outdated, unusable) = read.mapNotNull { (bundle, problem) -> problem?.let { bundle to it } }
            .partition { (bundle, problem) -> bundle.index != null && problem is Problem.BundleTooOld }
        if (outdated.isNotEmpty() || unusable.isNotEmpty()) {
            val stale = outdated.map { it.first.id }.toSet()
            val removed = unusable.map { it.first.id }.toSet()
            store.update { library ->
                library.copy(bundles = library.bundles.filterNot { it.id in removed }.map { if (it.id in stale) it.copy(checkedAtEpochMs = null) else it })
            }
            withContext(Dispatchers.IO) { unusable.forEach { (bundle) -> PlatformFile(bundle.path).delete(mustExist = false) } }
        }
        patchesState.value = read.filter { (_, problem) -> problem == null }.associate { (bundle, _, patches) -> bundle.id to patches }
        return unusable
    }

    private suspend fun loaded() = patchesState.filterNotNull().first()

    fun installed(): List<Bundle> = library.value.bundles

    fun paths(): List<String> = installed().map { it.path }

    /** The pinned official key plus every installed signer: confirmed API signers and confirmed third parties. */
    fun trust(): Trust = Trust(keys = (installed().map { it.id } + OfficialSignerKey).distinct())

    /** A new signer stops the sync at its bundle, since only one prompt can be [pending]; the rest wait for the next sync. */
    suspend fun sync(apiBaseUrl: String, scope: SyncScope): BundleSync = syncLock.withLock {
        loaded()
        syncingState.value = true
        try {
            val now = Clock.System.now()
            fun checks(bundle: Bundle) = when (scope) {
                SyncScope.All -> true
                SyncScope.Due -> bundle.checkedAtEpochMs?.let { Instant.fromEpochMilliseconds(it) + UpdateInterval <= now } ?: true
                SyncScope.None -> false
            }
            val failures = mutableListOf<SyncFailure>()
            suspend fun attempt(name: String, update: suspend () -> StagedBundle?): StagedBundle? = try {
                update()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failures += SyncFailure(name, error)
                null
            }
            val official = installed().firstOrNull { it.official }
            if (official == null || checks(official)) {
                attempt(official?.name ?: "Official patches") { syncOfficial(apiBaseUrl, official) }?.let { return@withLock BundleSync(it, failures) }
            }
            for (bundle in installed().filter { it.index != null && checks(it) }) {
                attempt(bundle.name) { syncIndex(bundle) }?.let { return@withLock BundleSync(it, failures) }
            }
            BundleSync(null, failures)
        } finally {
            syncingState.value = false
        }
    }

    private suspend fun syncOfficial(apiBaseUrl: String, installed: Bundle?): StagedBundle? {
        val key = fetchOfficialKey(apiBaseUrl)
        val prompt = officialSignerPrompt(apiBaseUrl, key, installed)
        val release = fetchOfficialRelease(apiBaseUrl).release
        installed?.let { markChecked(it.id) }
        if (installed != null && installed.id == key && installed.version == release.version) return null
        return stageUpdate(release, key, UpdateSource.Official(release.version), prompt)
    }

    private suspend fun syncIndex(bundle: Bundle): StagedBundle? {
        val url = checkNotNull(bundle.index)
        val index = fetchBundleIndex(url)
        val release = index.latest
        markChecked(bundle.id)
        val key = index.bundle.publicKey
        if (bundle.id == key && bundle.version == release.version) return null
        return stageUpdate(release, key, UpdateSource.Index(url, release.version), if (key == bundle.id) null else TrustPrompt.ChangedSigner(bundle.id))
    }

    private suspend fun markChecked(id: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        store.update { library -> library.copy(bundles = library.bundles.map { if (it.id == id) it.copy(checkedAtEpochMs = now) else it }) }
    }

    private suspend fun stageUpdate(release: ReleaseInfo, key: String, source: UpdateSource, prompt: TrustPrompt?): StagedBundle? {
        val staged = stageRelease(release, key, source, trust = if (prompt == null) Trust(keys = listOf(key)) else trust())
        return offer(if (prompt == null) staged else staged.withPrompt(prompt))
    }

    private suspend fun stageRelease(release: ReleaseInfo, key: String, source: UpdateSource, trust: Trust): StagedBundle {
        val file = stagingFile()
        httpDownload(release.downloadUrl, file)
        val staged = inspect(file, origin = release.downloadUrl, trust = trust, source = source)
        if (staged.metadata.publicKey != key) {
            discard(staged)
            error("${staged.metadata.name} is not signed by the key its publisher lists")
        }
        return staged
    }

    suspend fun stageDownload(url: String): StagedBundle {
        val file = stagingFile()
        httpDownload(url, file)
        if (withContext(Dispatchers.IO) { file.isArchive() }) return inspect(file, origin = url, trust = trust(), source = null)
        val index = try {
            parseBundleIndex(url, withContext(Dispatchers.IO) { file.source().buffered().use { it.readString() } })
        } finally {
            withContext(Dispatchers.IO) { file.delete() }
        }
        val release = index.latest
        return stageRelease(release, index.bundle.publicKey, UpdateSource.Index(url, release.version), trust())
    }

    suspend fun stageFile(source: PlatformFile): StagedBundle {
        val file = stagingFile()
        withContext(Dispatchers.IO) { source.copyTo(file) }
        return inspect(file, origin = source.name, trust = trust(), source = null)
    }

    /** Installs a trusted staged bundle; otherwise parks it as [pending] and returns it. A previous pending bundle is discarded. */
    suspend fun offer(staged: StagedBundle): StagedBundle? {
        if (staged.prompt == null) {
            install(staged)
            return null
        }
        pendingState.getAndUpdate { staged }?.let { discard(it) }
        return staged
    }

    suspend fun decide(trust: Boolean) {
        val staged = pendingState.getAndUpdate { null } ?: return
        if (trust) install(staged) else discard(staged)
    }

    /** Moves a staged bundle into the library. A signer confirmed by the user is inspected again with its key trusted so its patches load. */
    private suspend fun install(staged: StagedBundle) {
        loaded()
        val metadata = staged.metadata
        val patches = if (staged.prompt == null) staged.patches else ReseamSdk.inspect(
            InspectRequest(splitPaths = emptyList(), bundlePaths = listOf(staged.file.absolutePath()), trust = Trust(keys = listOf(metadata.publicKey))),
        ).patches
        val target = directory / "${metadata.publicKey}.reseam"
        withContext(Dispatchers.IO) {
            if (target.exists()) target.delete()
            staged.file.atomicMove(target)
        }
        val source = staged.source
        val bundle = Bundle(
            id = metadata.publicKey,
            name = metadata.name,
            author = metadata.author,
            description = metadata.description,
            version = source?.version,
            official = source is UpdateSource.Official,
            origin = staged.origin,
            path = target.absolutePath(),
            index = (source as? UpdateSource.Index)?.url,
            checkedAtEpochMs = source?.let { Clock.System.now().toEpochMilliseconds() },
        )
        val replaced = installed().filter { it.id != bundle.id && it.replacedBy(bundle) }
        store.update { library -> library.copy(bundles = library.bundles.filterNot { it.id == bundle.id || it.replacedBy(bundle) } + bundle) }
        patchesState.update { checkNotNull(it) - replaced.map { old -> old.id }.toSet() + (bundle.id to patches) }
        withContext(Dispatchers.IO) { replaced.forEach { PlatformFile(it.path).delete(mustExist = false) } }
    }

    suspend fun discard(staged: StagedBundle) = withContext(Dispatchers.IO) { staged.file.delete() }

    suspend fun remove(id: String) {
        loaded()
        val bundle = installed().firstOrNull { it.id == id && !it.official } ?: return
        store.update { it.copy(bundles = it.bundles.filterNot { existing -> existing.id == id }) }
        patchesState.update { checkNotNull(it) - id }
        withContext(Dispatchers.IO) { PlatformFile(bundle.path).delete(mustExist = false) }
    }

    private fun Bundle.replacedBy(next: Bundle) = (official && next.official) || (index != null && index == next.index)

    private suspend fun inspect(file: PlatformFile, origin: String, trust: Trust, source: UpdateSource?): StagedBundle {
        val response = try {
            ReseamSdk.inspect(InspectRequest(splitPaths = emptyList(), bundlePaths = listOf(file.absolutePath()), trust = trust))
        } catch (error: Exception) {
            withContext(Dispatchers.IO) { file.delete() }
            throw error
        }
        val metadata = response.bundles.single()
        metadata.problem?.takeUnless { it is Problem.UntrustedBundle }?.let { problem ->
            withContext(Dispatchers.IO) { file.delete() }
            throw SdkError(problem, problem.toString())
        }
        return StagedBundle(metadata, origin, if (metadata.trusted) null else TrustPrompt.UnknownSigner, file, response.patches, source)
    }

    private fun StagedBundle.withPrompt(prompt: TrustPrompt) = StagedBundle(metadata, origin, prompt, file, patches, source)

    private fun PlatformFile.isArchive(): Boolean = source().buffered().use { it.request(ZipMagic.size.toLong()) && it.readByteArray(ZipMagic.size).contentEquals(ZipMagic) }

    /** A staged bundle outlives the app when the engine aborts the process mid-inspect, so sweep before staging again. */
    private suspend fun stagingFile(): PlatformFile = withContext(Dispatchers.IO) {
        directory.createDirectories()
        directory.list().filter { it.name.startsWith(StagingPrefix) }.forEach { it.delete(mustExist = false) }
        directory / "$StagingPrefix${Uuid.random()}.reseam"
    }
}
