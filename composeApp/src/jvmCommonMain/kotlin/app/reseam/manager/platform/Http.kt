package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.asSource
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

private const val ConnectTimeoutMs = 15_000
private const val ReadTimeoutMs = 60_000
private const val DownloadBufferBytes = 64 * 1024

/** The body and headers are kept because some servers say why they refused there. */
class HttpStatusException(val status: Int, val url: String, val body: String, private val headers: Map<String, List<String>>) : IOException("HTTP $status for $url") {
    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()
}

data class HttpText(val status: Int, val body: String)

private fun open(url: String, headers: Map<String, String>, body: String? = null): HttpURLConnection {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.connectTimeout = ConnectTimeoutMs
    connection.readTimeout = ReadTimeoutMs
    connection.instanceFollowRedirects = true
    headers.forEach(connection::setRequestProperty)
    if (body != null) {
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.outputStream.use { it.write(body.encodeToByteArray()) }
    }
    if (connection.responseCode !in 200..299) {
        val error = HttpStatusException(
            connection.responseCode,
            url,
            connection.errorStream?.asSource()?.buffered()?.readByteArray()?.decodeToString().orEmpty(),
            connection.headerFields.filterKeys { it != null },
        )
        connection.disconnect()
        throw error
    }
    return connection
}

private fun HttpURLConnection.readText(): String = try {
    inputStream.asSource().buffered().readByteArray().decodeToString()
} finally {
    disconnect()
}

suspend fun httpText(url: String, headers: Map<String, String> = emptyMap(), body: String? = null): HttpText = withContext(Dispatchers.IO) {
    val connection = open(url, headers, body)
    HttpText(connection.responseCode, connection.readText())
}

suspend fun httpGetText(url: String, headers: Map<String, String> = emptyMap()): String = httpText(url, headers).body

suspend fun httpPostText(url: String, body: String, headers: Map<String, String> = emptyMap()): String = httpText(url, headers, body).body

suspend fun httpDownload(
    url: String,
    into: PlatformFile,
    headers: Map<String, String> = emptyMap(),
    onProgress: (written: Long, total: Long?) -> Unit = { _, _ -> },
): Unit = withContext(Dispatchers.IO) {
    val connection = open(url, headers)
    try {
        val total = connection.contentLengthLong.takeIf { it > 0 }
        connection.inputStream.use { input ->
            into.sink().buffered().use { sink ->
                val buffer = ByteArray(DownloadBufferBytes)
                var written = 0L
                while (true) {
                    ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    sink.write(buffer, 0, read)
                    written += read
                    onProgress(written, total)
                }
            }
        }
    } finally {
        connection.disconnect()
    }
}
