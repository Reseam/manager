package app.reseam.manager.platform

/**
 * The identity source requests go out with. A solved challenge is only honored for the user agent
 * that solved it, so where the user can solve one this must match that browser.
 */
interface SourceSession {
    val userAgent: String

    fun cookies(url: String): String?
}
