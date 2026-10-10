package app.reseam.manager.platform

interface SourceSession {
    val userAgent: String

    fun cookies(url: String): String?
}
