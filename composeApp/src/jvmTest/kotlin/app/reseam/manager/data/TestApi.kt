package app.reseam.manager.data

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress

/** The subset of the Reseam API the manager's official sync reads, plus a publisher's `patches.json`, served from one bundle file. */
class TestApi(var servedKey: String, var bundle: File, var version: String) : AutoCloseable {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v1/patches/keys") { exchange -> exchange.reply("""{"public_key":"$servedKey"}""".toByteArray()) }
        createContext("/v1/patches") { exchange -> exchange.reply(official().toByteArray()) }
        createContext("/patches.json") { exchange -> exchange.reply(index().toByteArray()) }
        createContext("/bundle") { exchange -> exchange.reply(bundle.readBytes()) }
        start()
    }

    private val root = "http://127.0.0.1:${server.address.port}"
    val baseUrl = "$root/v1"
    val bundleUrl = "$root/bundle"
    val indexUrl = "$root/patches.json"

    val missingApiUrl = "$root/none"

    private fun official() = """
        {"bundle":{"name":"test-patches","author":"Test","description":"","homepage":"","public_key":"$servedKey"},
         "release":{"version":"$version","created_at":"2026-09-05T12:00:00Z","description":"","download_url":"$bundleUrl","prerelease":false}}
    """.trimIndent()

    private fun index() = """
        {"bundle":{"name":"test-patches","author":"Test","description":"","public_key":"$servedKey"},
         "releases":[
           {"version":"$version-beta","created_at":"2026-09-06T12:00:00Z","description":"","download_url":"$root/missing","prerelease":true},
           {"version":"$version","created_at":"2026-09-05T12:00:00Z","description":"","download_url":"$bundleUrl","prerelease":false}
         ]}
    """.trimIndent()

    private fun com.sun.net.httpserver.HttpExchange.reply(body: ByteArray) {
        sendResponseHeaders(200, body.size.toLong())
        responseBody.use { it.write(body) }
    }

    override fun close() = server.stop(0)
}
