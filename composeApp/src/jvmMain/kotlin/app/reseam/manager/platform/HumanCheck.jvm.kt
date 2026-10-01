package app.reseam.manager.platform

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.SheetHeader
import ca.weblite.webview.JavascriptFunction
import ca.weblite.webview.swing.WebViewComponent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json

private const val ClearanceCookie = "cf_clearance="
private const val PageLoaded = "reseamPageLoaded"

@Composable
actual fun HumanCheck(url: String, onVerified: () -> Unit) {
    val verified by rememberUpdatedState(onVerified)
    val webView = remember { WebViewComponent.create() }
    DisposableEffect(webView) { onDispose(webView::dispose) }
    SheetHeader("Confirm you're human", "A quick check is needed before the download starts. It continues once the check passes.")
    SwingPanel(factory = { webView }, modifier = Modifier.fillMaxWidth().height(420.dp))
    LaunchedEffect(webView) {
        val stale = DesktopSourceSession.cookies(url)?.clearance()
        // Passing sets a fresh clearance cookie and reloads, and a loaded page means the engine is attached.
        val loads = Channel<Unit>(Channel.CONFLATED)
        webView.addJavascriptFunction(PageLoaded, JavascriptFunction { loads.trySend(Unit); "null" })
        webView.addOnBeforeLoad("addEventListener('DOMContentLoaded', () => $PageLoaded())")
        webView.setUrl(url)
        val cookies = loads.receiveAsFlow()
            .map { webView.getCookies(url).await().orEmpty() }
            .first { it.clearance().let { clearance -> clearance != null && clearance != stale } }
        DesktopSourceSession.verified(url, Json.decodeFromString(webView.evalAsync("return navigator.userAgent").await()), cookies)
        verified()
    }
}

private fun String.clearance() = split("; ").firstOrNull { it.startsWith(ClearanceCookie) }
