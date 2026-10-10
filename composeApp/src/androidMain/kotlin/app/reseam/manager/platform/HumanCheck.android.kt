package app.reseam.manager.platform

import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.reseam.manager.ui.theme.Radius

private const val ClearanceCookie = "cf_clearance="

@Composable
actual fun HumanCheck(url: String, onVerified: () -> Unit) {
    val verified by rememberUpdatedState(onVerified)
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(Radius.md)),
        factory = { context ->
            val cookies = CookieManager.getInstance()
            fun clearance() = cookies.getCookie(url)?.split("; ")?.firstOrNull { it.startsWith(ClearanceCookie) }
            val stale = clearance()
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                cookies.setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    // Passing sets a fresh clearance cookie and reloads; stop there so only the check is ever shown.
                    override fun onPageStarted(view: WebView, started: String, favicon: Bitmap?) {
                        if (clearance().let { it != null && it != stale }) {
                            view.stopLoading()
                            cookies.flush()
                            verified()
                        }
                    }
                }
                loadUrl(url)
            }
        },
        onRelease = WebView::destroy,
    )
}
