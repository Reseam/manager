package app.reseam.manager.platform

import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object DesktopSourceSession : SourceSession {
    private val cookiesByHost = ConcurrentHashMap<String, String>()

    @Volatile
    override var userAgent = "Mozilla/5.0 (${DesktopOperatingSystem.current.browserPlatform}; rv:140.0) Gecko/20100101 Firefox/140.0"
        private set

    override fun cookies(url: String): String? = cookiesByHost[URI(url).host]

    fun verified(url: String, userAgent: String, cookies: String) {
        this.userAgent = userAgent
        cookiesByHost[URI(url).host] = cookies
    }
}
