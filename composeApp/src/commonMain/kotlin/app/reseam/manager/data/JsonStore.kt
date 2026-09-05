package app.reseam.manager.data

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.json.Json

/** A single JSON document on disk mirrored into memory; writes are atomic and serialized. */
class JsonStore<T>(
    private val directory: PlatformFile,
    fileName: String,
    private val serializer: KSerializer<T>,
    default: T,
) {
    private val file = directory / fileName
    private val staging = directory / "$fileName.tmp"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val lock = Mutex()
    private val current = MutableStateFlow(if (file.exists()) json.decodeFromString(serializer, file.source().buffered().use { it.readString() }) else default)

    val state: StateFlow<T> = current.asStateFlow()

    suspend fun update(transform: (T) -> T): T = lock.withLock {
        val next = transform(current.value)
        withContext(Dispatchers.IO) {
            directory.createDirectories()
            staging.sink().buffered().use { it.writeString(json.encodeToString(serializer, next)) }
            staging.atomicMove(file)
        }
        current.value = next
        next
    }
}
