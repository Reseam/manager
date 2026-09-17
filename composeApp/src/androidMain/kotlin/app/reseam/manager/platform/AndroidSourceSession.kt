package app.reseam.manager.platform

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings

class AndroidSourceSession(private val context: Context) : SourceSession {
    override val userAgent: String by lazy { WebSettings.getDefaultUserAgent(context) }

    override fun cookies(url: String): String? = CookieManager.getInstance().getCookie(url)
}
