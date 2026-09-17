package app.reseam.manager.data

import app.reseam.manager.platform.HttpStatusException
import app.reseam.manager.platform.SourceSession
import app.reseam.manager.platform.httpDownload
import app.reseam.manager.platform.httpPostText
import app.reseam.manager.platform.httpText
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.IOException

const val DefaultDownloaderBaseUrl = "https://dl.reseam.app/v1"

/** Asks for whichever source the service considers best, so the client names none of them. */
const val DefaultSource = "default"

@Serializable
enum class BuildContainer {
    @SerialName("apk")
    Apk,

    @SerialName("bundle")
    Bundle,
}

@Serializable
data class Build(
    val id: String,
    val versionName: String,
    val versionCode: String? = null,
    val container: BuildContainer,
    val abis: List<String>,
    val minAndroid: String,
    val dpi: String,
)

@Serializable
data class SourceApp(val name: String)

@Serializable
data class AppBuilds(val app: SourceApp, val builds: List<Build>)

@Serializable
data class DownloadTarget(val url: String, val headers: Map<String, String>)

class HumanCheckRequired(val url: String) : IOException("The download needs you to confirm you're human")

@Serializable
private data class Query(
    val source: String,
    val op: String,
    @SerialName("package") val packageName: String? = null,
    val version: String? = null,
    val build: String? = null,
)

@Serializable
private data class Ask(val ticket: String, val responses: List<Page>)

@Serializable
private data class Request(
    val id: String,
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)

@Serializable
private data class Page(val id: String, val status: Int, val body: String)

@Serializable
private sealed interface Reply {
    @Serializable
    @SerialName("ok")
    data class Ok(val data: JsonElement) : Reply

    @Serializable
    @SerialName("fetch")
    data class Fetch(val ticket: String, val requests: List<Request>) : Reply
}

@Serializable
private data class ServiceError(val error: String)

private val ProtocolJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "status"
    explicitNulls = false
}

private val JsonHeaders = mapOf("Content-Type" to "application/json")

/** Pages the service cannot fetch itself come from here: a solved challenge only counts for the client that solved it. */
class Downloader(private val baseUrl: String, private val session: SourceSession, val source: String) {
    /** Questions stay sequential: bursts get the client challenged for a long while. */
    private val lock = Mutex()

    suspend fun builds(packageName: String, version: String?): AppBuilds =
        ask(Query(source, "builds", packageName = packageName, version = version), AppBuilds.serializer())

    suspend fun resolve(build: Build): DownloadTarget =
        ask(Query(source, "resolve", build = build.id), DownloadTarget.serializer())

    suspend fun download(target: DownloadTarget, into: PlatformFile, onProgress: (written: Long, total: Long?) -> Unit) =
        httpDownload(target.url, into, sourceHeaders(target.url) + target.headers, onProgress)

    private suspend fun <T> ask(query: Query, serializer: DeserializationStrategy<T>): T = lock.withLock {
        withContext(Dispatchers.IO) {
            var reply = post("$baseUrl/query", ProtocolJson.encodeToString(query))
            while (reply is Reply.Fetch) {
                val pages = reply.requests.map { fetch(it) }
                reply = post("$baseUrl/continue", ProtocolJson.encodeToString(Ask(reply.ticket, pages)))
            }
            ProtocolJson.decodeFromJsonElement(serializer, (reply as Reply.Ok).data)
        }
    }

    private suspend fun post(url: String, body: String): Reply = try {
        ProtocolJson.decodeFromString(httpPostText(url, body, JsonHeaders))
    } catch (refused: HttpStatusException) {
        throw IOException(refused.serviceMessage())
    }

    private suspend fun fetch(request: Request): Page {
        val headers = sourceHeaders(request.url) + request.headers
        val body = if (request.method == "POST") request.body.orEmpty() else null
        val page = try {
            httpText(request.url, headers, body)
        } catch (refused: HttpStatusException) {
            if (refused.header("cf-mitigated") == "challenge") throw HumanCheckRequired(request.url)
            throw refused
        }
        return Page(request.id, page.status, page.body)
    }

    private fun sourceHeaders(url: String): Map<String, String> = buildMap {
        put("User-Agent", session.userAgent)
        session.cookies(url)?.let { put("Cookie", it) }
    }
}

private fun HttpStatusException.serviceMessage(): String =
    runCatching { ProtocolJson.decodeFromString<ServiceError>(body).error }.getOrNull() ?: "HTTP $status for $url"
