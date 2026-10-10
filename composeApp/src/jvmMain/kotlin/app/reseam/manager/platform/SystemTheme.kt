package app.reseam.manager.platform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runInterruptible

private const val Portal = "org.freedesktop.portal.Desktop"
private const val PortalPath = "/org/freedesktop/portal/desktop"
private val ColorScheme = Regex("""'org\.freedesktop\.appearance', 'color-scheme', <uint32 (\d)>""")
private val ColorSchemeValue = Regex("""<uint32 (\d)>""")

/** Compose does not read the Linux desktop's preference, which the freedesktop settings portal publishes. */
@Composable
fun rememberSystemDarkTheme(): Boolean {
    val compose = isSystemInDarkTheme()
    if (DesktopOperatingSystem.current != DesktopOperatingSystem.Linux) return compose
    val dark by produceState(compose) { portalDarkTheme().collect { value = it } }
    return dark
}

private fun portalDarkTheme(): Flow<Boolean> = flow {
    val monitor = gdbus("monitor", "--session", "--dest", Portal, "--object-path", PortalPath) ?: return@flow
    try {
        monitor.inputStream.bufferedReader().use { lines ->
            while (true) {
                val line = runInterruptible { lines.readLine() } ?: break
                ColorScheme.find(line)?.let { emit(it.groupValues[1] == "1") }
            }
        }
    } finally {
        monitor.destroy()
    }
}
    .onStart { readColorScheme()?.let { emit(it) } }
    .distinctUntilChanged()
    .flowOn(Dispatchers.IO)

private fun readColorScheme(): Boolean? {
    val read = gdbus(
        "call", "--session", "--dest", Portal, "--object-path", PortalPath,
        "--method", "org.freedesktop.portal.Settings.ReadOne", "org.freedesktop.appearance", "color-scheme",
    ) ?: return null
    val output = read.inputStream.bufferedReader().use { it.readText() }
    return ColorSchemeValue.find(output).takeIf { read.waitFor() == 0 }?.let { it.groupValues[1] == "1" }
}

private fun gdbus(vararg arguments: String): Process? =
    try {
        ProcessBuilder("gdbus", *arguments).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    } catch (_: IOException) {
        null
    }
