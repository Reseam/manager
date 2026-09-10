package app.reseam.manager.data

import app.reseam.manager.platform.httpDownload
import app.reseam.manager.sdk.BundleMetadata
import app.reseam.manager.sdk.InspectRequest
import app.reseam.manager.sdk.PatchMetadata
import app.reseam.manager.sdk.Problem
import app.reseam.manager.sdk.ReseamException
import app.reseam.manager.sdk.ReseamSdk
import app.reseam.manager.sdk.Trust
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    val patches: List<PatchMetadata>,
)

@Serializable
data class BundleLibrary(
    val bundles: List<Bundle> = emptyList(),
    val officialCheckedAtEpochMs: Long? = null,
)

/** A bundle file inspected but not yet installed. Patches are empty while [prompt] is set: untrusted code is never loaded. */
class StagedBundle internal constructor(
    val metadata: BundleMetadata,
    val origin: String,
    val prompt: TrustPrompt?,
    internal val file: PlatformFile,
    internal val patches: List<PatchMetadata>,
    internal val officialVersion: String?,
)

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
private const val StagingPrefix = ".staging-"

class BundleRepository(
    private val store: JsonStore<BundleLibrary>,
    private val directory: PlatformFile,
) {
    val library: StateFlow<BundleLibrary> = store.state
    val bundles: Flow<List<Bundle>> = library.map { it.bundles }
    val syncing: StateFlow<Boolean> get() = syncingState

    /** The one staged bundle waiting for the user's trust decision, from any flow. */
    val pending: StateFlow<StagedBundle?> get() = pendingState

    private val syncingState = MutableStateFlow(false)
    private val pendingState = MutableStateFlow<StagedBundle?>(null)

    fun installed(): List<Bundle> = library.value.bundles

    fun paths(): List<String> = installed().map { it.path }

    /** The pinned official key plus every installed signer: confirmed API signers and confirmed third parties. */
    fun trust(): Trust = Trust(keys = (installed().map { it.id } + OfficialSignerKey).distinct())

    /**
     * Installs the newest stable official bundle when none is installed, the daily check is due, or [force] is set.
     * Returns the staged bundle when its signer needs the user's confirmation first; it is already [pending].
     */
    suspend fun syncOfficial(apiBaseUrl: String, force: Boolean): StagedBundle? {
        val current = library.value
        val installed = current.bundles.firstOrNull { it.official }
        val checkedAt = current.officialCheckedAtEpochMs?.let(Instant::fromEpochMilliseconds)
        val due = installed == null || force || checkedAt == null || checkedAt + 24.hours < Clock.System.now()
        if (!due) return null
        syncingState.value = true
        try {
            val key = fetchOfficialKey(apiBaseUrl)
            val prompt = officialSignerPrompt(apiBaseUrl, key, installed)
            val index = fetchOfficialRelease(apiBaseUrl)
            store.update { it.copy(officialCheckedAtEpochMs = Clock.System.now().toEpochMilliseconds()) }
            if (installed != null && installed.id == key && installed.version == index.release.version) return null
            val file = stagingFile()
            httpDownload(index.release.downloadUrl, file)
            val trust = if (prompt == null) Trust(keys = listOf(key)) else trust()
            val staged = inspect(file, origin = index.release.downloadUrl, trust = trust, officialVersion = index.release.version)
            if (staged.metadata.publicKey != key) {
                discard(staged)
                error("The official bundle is not signed by the key the API published")
            }
            return offer(if (prompt == null) staged else staged.withPrompt(prompt))
        } finally {
            syncingState.value = false
        }
    }

    suspend fun stageDownload(url: String): StagedBundle {
        val file = stagingFile()
        httpDownload(url, file)
        return inspect(file, origin = url, trust = trust(), officialVersion = null)
    }

    suspend fun stageFile(source: PlatformFile): StagedBundle {
        val file = stagingFile()
        withContext(Dispatchers.IO) { source.copyTo(file) }
        return inspect(file, origin = source.name, trust = trust(), officialVersion = null)
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
        val metadata = staged.metadata
        val patches = if (staged.prompt == null) staged.patches else ReseamSdk.inspect(
            InspectRequest(bundlePaths = listOf(staged.file.absolutePath()), trust = Trust(keys = listOf(metadata.publicKey))),
        ).patches
        val target = directory / "${metadata.publicKey}.reseam"
        withContext(Dispatchers.IO) {
            if (target.exists()) target.delete()
            staged.file.atomicMove(target)
        }
        val official = staged.officialVersion != null
        val bundle = Bundle(
            id = metadata.publicKey,
            name = metadata.name,
            author = metadata.author,
            description = metadata.description,
            version = staged.officialVersion,
            official = official,
            origin = staged.origin,
            path = target.absolutePath(),
            patches = patches,
        )
        val replaced = installed().filter { it.id != bundle.id && official && it.official }
        store.update { library -> library.copy(bundles = library.bundles.filterNot { it.id == bundle.id || (official && it.official) } + bundle) }
        withContext(Dispatchers.IO) { replaced.forEach { PlatformFile(it.path).delete(mustExist = false) } }
    }

    suspend fun discard(staged: StagedBundle) = withContext(Dispatchers.IO) { staged.file.delete() }

    suspend fun remove(id: String) {
        val bundle = installed().firstOrNull { it.id == id && !it.official } ?: return
        store.update { it.copy(bundles = it.bundles.filterNot { existing -> existing.id == id }) }
        withContext(Dispatchers.IO) { PlatformFile(bundle.path).delete(mustExist = false) }
    }

    private suspend fun inspect(file: PlatformFile, origin: String, trust: Trust, officialVersion: String?): StagedBundle {
        val response = try {
            ReseamSdk.inspect(InspectRequest(bundlePaths = listOf(file.absolutePath()), trust = trust))
        } catch (error: Exception) {
            withContext(Dispatchers.IO) { file.delete() }
            throw error
        }
        val metadata = response.bundles.single()
        metadata.problem?.takeUnless { it is Problem.UntrustedBundle }?.let { problem ->
            withContext(Dispatchers.IO) { file.delete() }
            throw ReseamException(problem, problem.toString())
        }
        return StagedBundle(metadata, origin, if (metadata.trusted) null else TrustPrompt.UnknownSigner, file, response.patches, officialVersion)
    }

    private fun StagedBundle.withPrompt(prompt: TrustPrompt) = StagedBundle(metadata, origin, prompt, file, patches, officialVersion)

    /** A staged bundle outlives the app when the engine aborts the process mid-inspect, so sweep before staging again. */
    private suspend fun stagingFile(): PlatformFile = withContext(Dispatchers.IO) {
        directory.createDirectories()
        directory.list().filter { it.name.startsWith(StagingPrefix) }.forEach { it.delete(mustExist = false) }
        directory / "$StagingPrefix${Uuid.random()}.reseam"
    }
}
