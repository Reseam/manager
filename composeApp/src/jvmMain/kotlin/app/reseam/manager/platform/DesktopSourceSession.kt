package app.reseam.manager.platform

object DesktopSourceSession : SourceSession {
    override val userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0"

    override fun cookies(url: String): String? = null
}
