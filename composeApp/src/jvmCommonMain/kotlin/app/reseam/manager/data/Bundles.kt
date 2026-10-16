package app.reseam.manager.data

import app.reseam.manager.platform.HttpStatusException
import app.reseam.manager.platform.Transfer
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Serializable
data class Bundle(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String?,
    val official: Boolean,
    val origin: String,
    val path: String,
    val index: String? = null,
    val whatsNew: WhatsNew? = null,
    val enabled: Boolean = true,
) {
    val followsUpdates: Boolean get() = official || index != null
}

@Serializable
data class BundleRef(val id: String, val name: String, val version: String?, val origin: String)

val Bundle.ref: BundleRef get() = BundleRef(id, name, version, origin)

@Serializable
data class BundleLibrary(val bundles: List<Bundle> = emptyList())

sealed interface UpdateSource {
    val version: String

    data class Official(override val version: String) : UpdateSource

    data class Index(val url: String, override val version: String) : UpdateSource
}

class StagedBundle internal constructor(
    val metadata: BundleMetadata,
    val origin: String,
    val prompt: TrustPrompt?,
    internal val file: PlatformFile,
    val patches: List<PatchMetadata>,
    val source: UpdateSource?,
    val notes: List<ReleaseNote> = emptyList(),
)

data class SyncFailure(val bundle: String, val error: Exception)

data class BundleSync(val prompt: StagedBundle?, val failures: List<SyncFailure>, val updated: Int = 0)

sealed interface BundleUpdatePhase {
    data class Downloading(val transfer: Transfer) : BundleUpdatePhase
    data object Installing : BundleUpdatePhase
}

data class BundleUpdate(val name: String, val version: String, val phase: BundleUpdatePhase = BundleUpdatePhase.Downloading(Transfer(0, null)))

data class IndexLink(val url: String, val index: BundleIndex)

sealed interface LinkTarget {
    data class Bundle(val staged: StagedBundle) : LinkTarget

    data class Index(val link: IndexLink) : LinkTarget
}

@OptIn(ExperimentalUuidApi::class)
private const val StagingPrefix = ".staging-"

private const val OfficialBundleName = "Official patches"

private val ZipMagic = byteArrayOf(0x50, 0x4b, 0x03, 0x04)

class BundleRepository(
    private val store: JsonStore<BundleLibrary>,
    private val directory: PlatformFile,
) {
    val library: StateFlow<BundleLibrary> = store.state
    val bundles: Flow<List<Bundle>> = library.map { it.bundles }
    val syncing: StateFlow<Boolean> get() = syncingState

    val patches: StateFlow<Map<String, List<PatchMetadata>>?> get() = patchesState

    /** Patches of enabled sources, the ones offered when patching. */
    val active: Flow<Map<String, List<PatchMetadata>>?> get() = combine(patchesState, library) { patches, library ->
        patches?.filterKeys { id -> library.bundles.any { it.id == id && it.enabled } }
    }
    val catalog: Flow<Catalog?> get() = active.map { it?.let(Catalog::of) }

    val updating: StateFlow<BundleUpdate?> get() = updatingState

    val pending: StateFlow<StagedBundle?> get() = pendingState

    val offers: StateFlow<List<BundleOffer>> get() = offersState

    private val syncingState = MutableStateFlow(false)
    private val pendingState = MutableStateFlow<StagedBundle?>(null)
    private val updatingState = MutableStateFlow<BundleUpdate?>(null)
    private val offersState = MutableStateFlow<List<BundleOffer>>(emptyList())
    private val patchesState = MutableStateFlow<Map<String, List<PatchMetadata>>?>(null)
    private val syncLock = Mutex()

    suspend fun load(): List<Pair<Bundle, Problem>> {
        val read = installed().map { bundle ->
            val response = ReseamSdk.inspect(InspectRequest(splitPaths = emptyList(), bundlePaths = listOf(bundle.path), trust = Trust(keys = listOf(bundle.id))))
            Triple(bundle, response.bundles.single().problem, response.patches)
        }
        val unusable = read.mapNotNull { (bundle, problem) -> problem?.let { bundle to it } }
            .filterNot { (bundle, problem) -> bundle.followsUpdates && problem is Problem.BundleTooOld }
        if (unusable.isNotEmpty()) {
            val removed = unusable.map { it.first.id }.toSet()
            store.update { library -> library.copy(bundles = library.bundles.filterNot { it.id in removed }) }
            withContext(Dispatchers.IO) { unusable.forEach { (bundle) -> PlatformFile(bundle.path).delete(mustExist = false) } }
        }
        patchesState.value = read.filter { (_, problem) -> problem == null }.associate { (bundle, _, patches) -> bundle.id to patches }
        return unusable
    }

    private suspend fun loaded() = patchesState.filterNotNull().first()

    fun installed(): List<Bundle> = library.value.bundles

    fun usable(): List<Bundle> = installed().filter { it.enabled && it.id in patchesState.value.orEmpty() }

    fun trust(): Trust = Trust(keys = (installed().map { it.id } + OfficialSignerKey).distinct())

    suspend fun sync(apiBaseUrl: String, install: Boolean, only: String? = null): BundleSync = syncLock.withLock {
        val loaded = loaded().keys
        syncingState.value = true
        try {
            val failures = mutableListOf<SyncFailure>()
            val offers = mutableListOf<BundleOffer>()
            var updated = 0
            suspend fun attempt(name: String, check: suspend () -> BundleOffer?, installNow: Boolean): StagedBundle? = try {
                check()?.let { offer ->
                    if (installNow) stage(offer).also { if (it == null) updated++ } else null.also { offers += offer }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failures += SyncFailure(name, error)
                null
            }
            fun wanted(bundle: Bundle?) = if (only == null) bundle?.enabled != false else bundle?.id == only
            val official = installed().firstOrNull { it.official }
            if (wanted(official)) {
                attempt(official?.name ?: OfficialBundleName, { checkOfficial(apiBaseUrl, official) }, installNow = install || official?.id !in loaded)
                    ?.let { return@withLock BundleSync(it, failures, updated) }
            }
            for (bundle in installed().filter { it.index != null && wanted(it) }) {
                attempt(bundle.name, { checkIndex(bundle) }, installNow = install || bundle.id !in loaded)?.let { return@withLock BundleSync(it, failures, updated) }
            }
            offersState.update { previous -> if (only == null) offers else previous.filterNot { it.bundleId == only } + offers }
            BundleSync(null, failures, updated)
        } finally {
            syncingState.value = false
        }
    }

    suspend fun update(offer: BundleOffer): StagedBundle? = syncLock.withLock {
        offersState.update { offers -> offers - offer }
        stage(offer)
    }

    suspend fun releaseNotes(bundle: Bundle, apiBaseUrl: String): List<ReleaseNote> = when {
        bundle.official -> fetchOfficialHistory(apiBaseUrl).filterNot { it.prerelease }.map { it.note() }
        bundle.index != null -> fetchBundleIndex(bundle.index).releases.filterNot { it.prerelease }.map { it.note() }
        else -> emptyList()
    }

    suspend fun dismissWhatsNew(id: String) {
        store.update { library -> library.copy(bundles = library.bundles.map { if (it.id == id) it.copy(whatsNew = null) else it }) }
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        store.update { library -> library.copy(bundles = library.bundles.map { if (it.id == id) it.copy(enabled = enabled) else it }) }
        if (!enabled) offersState.update { offers -> offers.filterNot { it.bundleId == id } }
    }

    private suspend fun checkOfficial(apiBaseUrl: String, installed: Bundle?): BundleOffer? {
        val key = fetchOfficialKey(apiBaseUrl)
        val prompt = officialSignerPrompt(apiBaseUrl, key, installed)
        val version = fetchOfficialVersion(apiBaseUrl)
        if (installed != null && installed.id == key && installed.version == version) return null
        val name = installed?.name ?: OfficialBundleName
        val releases = fetchOfficialHistory(apiBaseUrl).filterNot { it.prerelease }
        val release = releases.firstOrNull { it.version == version } ?: releases.firstOrNull() ?: throw Failure.NoStableRelease(name)
        return BundleOffer(installed?.id ?: key, name, release, key, UpdateSource.Official(release.version), prompt, releases.notesSince(installed?.version))
    }

    private suspend fun checkIndex(bundle: Bundle): BundleOffer? {
        val url = checkNotNull(bundle.index)
        val index = fetchBundleIndex(url)
        val release = index.latest
        val key = index.bundle.publicKey
        if (bundle.id == key && bundle.version == release.version) return null
        val prompt = if (key == bundle.id) null else TrustPrompt.ChangedSigner(bundle.id)
        return BundleOffer(bundle.id, bundle.name, release, key, UpdateSource.Index(url, release.version), prompt, index.releases.notesSince(bundle.version))
    }

    private suspend fun stage(offer: BundleOffer): StagedBundle? {
        val update = BundleUpdate(offer.name, offer.release.version)
        updatingState.value = update
        try {
            val trust = if (offer.prompt == null) Trust(keys = listOf(offer.key)) else trust()
            val staged = stageRelease(offer.name, offer.release, offer.key, offer.source, trust, offer.notes) { updatingState.value = update.copy(phase = BundleUpdatePhase.Downloading(it)) }
            updatingState.value = update.copy(phase = BundleUpdatePhase.Installing)
            return offer(if (offer.prompt == null) staged else staged.withPrompt(offer.prompt))
        } finally {
            updatingState.value = null
        }
    }

    private suspend fun stageRelease(
        publisher: String,
        release: ReleaseInfo,
        key: String,
        source: UpdateSource,
        trust: Trust,
        notes: List<ReleaseNote> = emptyList(),
        onProgress: (Transfer) -> Unit = {},
    ): StagedBundle {
        val file = stagingFile()
        try {
            httpDownload(release.downloadUrl, file, onProgress = onProgress)
        } catch (error: HttpStatusException) {
            withContext(Dispatchers.IO) { file.delete(mustExist = false) }
            throw if (error.status in 400..499) Failure.ReleaseUnavailable(publisher) else error
        }
        val staged = inspect(file, origin = release.downloadUrl, trust = trust, source = source, notes = notes)
        if (staged.metadata.publicKey != key) {
            discard(staged)
            throw Failure.NotSignedByPublisher(staged.metadata.name)
        }
        return staged
    }

    suspend fun stageIndex(link: IndexLink): StagedBundle {
        val release = link.index.latest
        return stageRelease(link.index.bundle.name, release, link.index.bundle.publicKey, UpdateSource.Index(link.url, release.version), trust())
    }

    /** Reads what a shared link points at: a bundle, staged at once, or a publisher index, from the link or from a repository page's latest release. */
    suspend fun resolveLink(link: String): LinkTarget {
        val trimmed = link.trim()
        val url = (if ("://" in trimmed) trimmed else "https://$trimmed").toHttpUrlOrNull() ?: throw Failure.InvalidLink(trimmed)
        val file = stagingFile()
        try {
            httpDownload(url.toString(), file)
        } catch (error: HttpStatusException) {
            withContext(Dispatchers.IO) { file.delete(mustExist = false) }
            throw if (error.status in 400..499) Failure.NoPatchSourceFound(url.toString()) else error
        } catch (_: Failure.ConnectionLost) {
            withContext(Dispatchers.IO) { file.delete(mustExist = false) }
            throw Failure.LinkUnreachable(url.host)
        }
        if (withContext(Dispatchers.IO) { file.isArchive() }) return LinkTarget.Bundle(inspect(file, origin = url.toString(), trust = trust(), source = null))
        val text = try {
            withContext(Dispatchers.IO) { file.source().buffered().use { it.readString() } }
        } finally {
            withContext(Dispatchers.IO) { file.delete() }
        }
        val index = try {
            parseBundleIndex(url.toString(), text)
        } catch (notIndex: Failure.NotABundle) {
            return LinkTarget.Index(latestReleaseIndex(url) ?: throw notIndex)
        }
        return LinkTarget.Index(IndexLink(url.toString(), index))
    }

    /** Null when [repository] can't be a repository page; throws when it can but no latest release carries an index. */
    private suspend fun latestReleaseIndex(repository: HttpUrl): IndexLink? {
        val candidates = latestIndexLinks(repository).ifEmpty { return null }
        for (candidate in candidates) {
            val index = try {
                fetchBundleIndex(candidate)
            } catch (_: HttpStatusException) {
                continue
            } catch (_: Failure.NotABundle) {
                continue
            }
            return IndexLink(candidate, index)
        }
        throw Failure.NoPatchSourceFound(repository.toString())
    }

    suspend fun stageFile(source: PlatformFile): StagedBundle {
        val file = stagingFile()
        withContext(Dispatchers.IO) { source.copyTo(file) }
        return inspect(file, origin = source.name, trust = trust(), source = null)
    }

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

    suspend fun install(staged: StagedBundle) {
        loaded()
        val metadata = staged.metadata
        val patches = staged.patches
        val target = directory / "${metadata.publicKey}.reseam"
        withContext(Dispatchers.IO) {
            if (target.exists()) target.delete()
            staged.file.atomicMove(target)
        }
        val source = staged.source
        val previous = installed().firstOrNull { it.id == metadata.publicKey || (source is UpdateSource.Official && it.official) || (source is UpdateSource.Index && it.index == source.url) }
        val changes = if (source == null || previous == null || previous.version == source.version) null else WhatsNew(source.version, staged.notes).takeUnless { it.isEmpty }
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
            whatsNew = changes?.let { previous?.whatsNew?.then(it) ?: it } ?: previous?.whatsNew,
            enabled = previous?.enabled ?: true,
        )
        val replaced = installed().filter { it.id != bundle.id && it.replacedBy(bundle) }
        store.update { library -> library.copy(bundles = library.bundles.filterNot { it.id == bundle.id || it.replacedBy(bundle) } + bundle) }
        offersState.update { offers -> offers.filterNot { it.bundleId == previous?.id || it.bundleId == bundle.id } }
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

    private suspend fun inspect(file: PlatformFile, origin: String, trust: Trust, source: UpdateSource?, notes: List<ReleaseNote> = emptyList()): StagedBundle {
        val response = try {
            ReseamSdk.inspect(InspectRequest(splitPaths = emptyList(), bundlePaths = listOf(file.absolutePath()), trust = trust))
        } catch (error: Exception) {
            withContext(Dispatchers.IO) { file.delete() }
            throw error
        }
        val metadata = response.bundles.single()
        metadata.problem?.let { problem ->
            withContext(Dispatchers.IO) { file.delete() }
            throw SdkError(problem, problem.toString())
        }
        return StagedBundle(metadata, origin, if (metadata.trusted) null else TrustPrompt.UnknownSigner, file, response.patches, source, notes)
    }

    private fun StagedBundle.withPrompt(prompt: TrustPrompt) = StagedBundle(metadata, origin, prompt, file, patches, source, notes)

    private fun PlatformFile.isArchive(): Boolean = source().buffered().use { it.request(ZipMagic.size.toLong()) && it.readByteArray(ZipMagic.size).contentEquals(ZipMagic) }

    /** A staged bundle outlives the app when the engine aborts the process mid-inspect, so sweep before staging again. */
    private suspend fun stagingFile(): PlatformFile = withContext(Dispatchers.IO) {
        directory.createDirectories()
        directory.list().filter { it.name.startsWith(StagingPrefix) }.forEach { it.delete(mustExist = false) }
        directory / "$StagingPrefix${Uuid.random()}.reseam"
    }
}
