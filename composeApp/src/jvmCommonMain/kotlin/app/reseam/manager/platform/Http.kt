package app.reseam.manager.platform

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.time.Duration

private const val DownloadBufferBytes = 64 * 1024

/** OkHttp tries every address a host resolves to, so one unreachable edge address doesn't fail the request. */
private val client = OkHttpClient.Builder()
    .connectTimeout(Duration.ofSeconds(15))
    .readTimeout(Duration.ofSeconds(60))
    .build()

/** The body and headers are kept because some servers say why they refused there. */
class HttpStatusException(val status: Int, val url: String, val body: String, private val headers: Headers) : IOException("HTTP $status for $url") {
    fun header(name: String): String? = headers[name]
}

data class HttpText(val status: Int, val body: String)

private suspend fun <T> send(url: String, headers: Map<String, String>, body: String?, read: suspend (Response) -> T): T = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(url)
        .headers(headers.toHeaders())
        .apply { if (body != null) post(body.toRequestBody()) }
        .build()
    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw HttpStatusException(response.code, url, response.body.string(), response.headers)
        read(response)
    }
}

suspend fun httpText(url: String, headers: Map<String, String> = emptyMap(), body: String? = null): HttpText =
    send(url, headers, body) { HttpText(it.code, it.body.string()) }

suspend fun httpGetText(url: String, headers: Map<String, String> = emptyMap()): String = httpText(url, headers).body

suspend fun httpPostText(url: String, body: String, headers: Map<String, String> = emptyMap()): String = httpText(url, headers, body).body

suspend fun httpDownload(
    url: String,
    into: PlatformFile,
    headers: Map<String, String> = emptyMap(),
    onProgress: (written: Long, total: Long?) -> Unit = { _, _ -> },
): Unit = send(url, headers, null) { response ->
    val total = response.body.contentLength().takeIf { it > 0 }
    response.body.byteStream().use { input ->
        into.sink().buffered().use { sink ->
            val buffer = ByteArray(DownloadBufferBytes)
            var written = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                sink.write(buffer, 0, read)
                written += read
                onProgress(written, total)
            }
        }
    }
}
