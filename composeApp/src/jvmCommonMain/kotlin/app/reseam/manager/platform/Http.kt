package app.reseam.manager.platform

import app.reseam.manager.data.Failure
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.sink
import java.io.File
import java.io.IOException
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import okhttp3.Cache
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private const val DownloadBufferBytes = 64 * 1024

private const val ProgressStepBytes = 256L * 1024

data class Transfer(val written: Long, val total: Long?) {
    val fraction: Float? get() = total?.let { written.toFloat() / it }
}

private const val TextCacheBytes = 10L * 1024 * 1024

private val client = OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(15))
    .readTimeout(Duration.ofSeconds(60))
    .build()

private var textClient = client

/**
 * Keeps text responses (release indexes, announcements) so a repeated check sends their ETag and gets a 304.
 * Downloads stay uncached, so bundles and installers never push the indexes out.
 */
fun cacheTextResponses(directory: PlatformFile) {
    textClient = client.newBuilder().cache(Cache(File(directory.absolutePath()), TextCacheBytes)).build()
}

class HttpStatusException(val status: Int, val url: String, val body: String, private val headers: Headers) : IOException("HTTP $status for $url") {
    fun header(name: String): String? = headers[name]
}

data class HttpText(val status: Int, val body: String)

private suspend fun <T> send(http: OkHttpClient, url: String, headers: Map<String, String>, body: String?, read: suspend (Response) -> T): T = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(url)
        .headers(headers.toHeaders())
        .apply { if (body != null) post(body.toRequestBody()) }
        .build()
    network { http.newCall(request).execute() }.use { response ->
        if (!response.isSuccessful) throw HttpStatusException(response.code, url, network { response.body.string() }, response.headers)
        read(response)
    }
}

/** Marks failures of the connection itself, so they read apart from local file errors. */
private inline fun <T> network(block: () -> T): T = try {
    block()
} catch (error: IOException) {
    throw Failure.ConnectionLost(error.message)
}

suspend fun httpText(url: String, headers: Map<String, String> = emptyMap(), body: String? = null): HttpText =
    send(textClient, url, headers, body) { HttpText(it.code, network { it.body.string() }) }

suspend fun httpGetText(url: String, headers: Map<String, String> = emptyMap()): String = httpText(url, headers).body

suspend fun httpPostText(url: String, body: String, headers: Map<String, String> = emptyMap()): String = httpText(url, headers, body).body

suspend fun httpDownload(
    url: String,
    into: PlatformFile,
    headers: Map<String, String> = emptyMap(),
    onProgress: (Transfer) -> Unit = {},
): Unit = send(client, url, headers, null) { response ->
    val total = response.body.contentLength().takeIf { it > 0 }
    response.body.byteStream().use { input ->
        into.sink().buffered().use { sink ->
            val buffer = ByteArray(DownloadBufferBytes)
            var written = 0L
            onProgress(Transfer(written, total))
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = network { input.read(buffer) }
                if (read < 0) break
                sink.write(buffer, 0, read)
                val step = written / ProgressStepBytes
                written += read
                if (written / ProgressStepBytes != step) onProgress(Transfer(written, total))
            }
            onProgress(Transfer(written, total))
        }
    }
}
