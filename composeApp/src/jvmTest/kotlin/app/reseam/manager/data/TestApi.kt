package app.reseam.manager.data

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetSocketAddress

/** The subset of the Reseam API the manager's official sync reads, served from one bundle file. */
class TestApi(private val servedKey: String, private val bundle: File, private val version: String) : AutoCloseable {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/v1/patches/keys") { exchange -> exchange.reply("""{"public_key":"$servedKey"}""".toByteArray()) }
        createContext("/v1/patches") { exchange -> exchange.reply(index().toByteArray()) }
        createContext("/bundle") { exchange -> exchange.reply(bundle.readBytes()) }
        start()
    }

    val baseUrl = "http://127.0.0.1:${server.address.port}/v1"
    val bundleUrl = "http://127.0.0.1:${server.address.port}/bundle"

    private fun index() = """
        {"bundle":{"name":"test-patches","author":"Test","description":"","homepage":"","public_key":"$servedKey"},
         "release":{"version":"$version","created_at":"2026-09-05T12:00:00Z","description":"","download_url":"$bundleUrl","prerelease":false}}
    """.trimIndent()

    private fun com.sun.net.httpserver.HttpExchange.reply(body: ByteArray) {
        sendResponseHeaders(200, body.size.toLong())
        responseBody.use { it.write(body) }
    }

    override fun close() = server.stop(0)
}
