package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.asSource
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

private const val ConnectTimeoutMs = 15_000
private const val ReadTimeoutMs = 60_000

private fun open(url: String): HttpURLConnection {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.connectTimeout = ConnectTimeoutMs
    connection.readTimeout = ReadTimeoutMs
    connection.instanceFollowRedirects = true
    if (connection.responseCode !in 200..299) {
        connection.disconnect()
        throw IOException("HTTP ${connection.responseCode} for $url")
    }
    return connection
}

actual suspend fun httpGetText(url: String): String = withContext(Dispatchers.IO) {
    val connection = open(url)
    try {
        connection.inputStream.asSource().buffered().readByteArray().decodeToString()
    } finally {
        connection.disconnect()
    }
}

actual suspend fun httpDownload(url: String, into: PlatformFile): Unit = withContext(Dispatchers.IO) {
    val connection = open(url)
    try {
        connection.inputStream.asSource().buffered().use { source ->
            into.sink().buffered().use { sink -> source.transferTo(sink) }
        }
    } finally {
        connection.disconnect()
    }
}
