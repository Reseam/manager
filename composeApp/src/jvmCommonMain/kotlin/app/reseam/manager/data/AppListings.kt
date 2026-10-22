package app.reseam.manager.data

import app.reseam.manager.platform.httpDownload
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import java.security.MessageDigest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private val NamedMaxAge = 7.days
private val UnnamedMaxAge = 1.days

/** A name and icon for an app the user has no copy of. */
@Serializable
data class AppListing(val name: String, val iconPath: String? = null)

@Serializable
data class AppListingLibrary(
    val apps: Map<String, AppListing> = emptyMap(),
    val checkedAtEpochMs: Map<String, Long> = emptyMap(),
)

@OptIn(ExperimentalTime::class)
class AppListings(
    private val store: JsonStore<AppListingLibrary>,
    private val icons: PlatformFile,
    private val downloader: Downloader,
) {
    val listings: Flow<Map<String, AppListing>> = store.state.map { it.apps }

    suspend fun refresh(packageNames: Collection<String>) {
        val now = Clock.System.now().toEpochMilliseconds()
        val library = store.state.value
        val due = packageNames.filter { packageName ->
            val maxAge = if (packageName in library.apps) NamedMaxAge else UnnamedMaxAge
            now - (library.checkedAtEpochMs[packageName] ?: 0) > maxAge.inWholeMilliseconds
        }
        if (due.isEmpty()) return
        val named = downloader.apps(due).mapValues { (packageName, info) ->
            AppListing(info.name, info.icon?.let { storeIcon(it) } ?: library.apps[packageName]?.iconPath)
        }
        val next = store.update {
            it.copy(
                apps = it.apps + named,
                checkedAtEpochMs = it.checkedAtEpochMs + due.associateWith { now })
        }
        removeUnusedIcons(next.apps.values.mapNotNull { it.iconPath }.toSet())
    }

    /** An icon that fails to load leaves the app without one rather than failing the names. */
    private suspend fun storeIcon(url: String): String? {
        val id = MessageDigest.getInstance("SHA-256").digest(url.encodeToByteArray()).toHexString()
        val file = icons / id
        val staged = icons / "$id.part"
        return try {
            withContext(Dispatchers.IO) { icons.createDirectories() }
            if (!withContext(Dispatchers.IO) { file.exists() }) {
                httpDownload(url, staged)
                withContext(Dispatchers.IO) { staged.atomicMove(file) }
            }
            file.absolutePath()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            withContext(Dispatchers.IO) { staged.delete(mustExist = false) }
            null
        }
    }

    private suspend fun removeUnusedIcons(used: Set<String>) = withContext(Dispatchers.IO) {
        if (icons.exists()) icons.list().filter { it.absolutePath() !in used }.forEach { it.delete(mustExist = false) }
    }
}
