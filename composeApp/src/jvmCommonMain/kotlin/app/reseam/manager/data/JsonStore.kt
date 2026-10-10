package app.reseam.manager.data

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

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
    private val current = MutableStateFlow(read() ?: default)

    val state: StateFlow<T> = current.asStateFlow()

    /** A document this version can't read is set aside rather than crashing every launch. */
    private fun read(): T? {
        if (!file.exists()) return null
        return try {
            json.decodeFromString(serializer, file.source().buffered().use { it.readString() })
        } catch (_: SerializationException) {
            Files.move(Path.of(file.absolutePath()), Path.of((directory / "${file.name}.unreadable").absolutePath()), StandardCopyOption.REPLACE_EXISTING)
            null
        }
    }

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
