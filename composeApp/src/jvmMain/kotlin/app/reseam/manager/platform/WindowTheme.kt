package app.reseam.manager.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.reseam.manager.ui.theme.ReseamDarkColors
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.awt.Window

private const val UseImmersiveDarkMode = 20
private const val CaptionColor = 35
private const val TextColor = 36

private interface DwmApi : StdCallLibrary {
    fun DwmSetWindowAttribute(window: Pointer, attribute: Int, value: IntByReference, size: Int): Int
}

/** Keeps native Windows chrome consistent with the app's dark theme. Other platforms own their chrome. */
fun applyWindowTheme(window: Window) {
    if (DesktopOperatingSystem.current != DesktopOperatingSystem.Windows) return
    val dwm = Native.load("dwmapi", DwmApi::class.java)
    val handle = Native.getWindowPointer(window)
    dwm.DwmSetWindowAttribute(handle, UseImmersiveDarkMode, IntByReference(1), Int.SIZE_BYTES)
    // Caption and text colors are available from Windows 11. Older Windows ignores these attributes.
    dwm.DwmSetWindowAttribute(handle, CaptionColor, IntByReference(ReseamDarkColors.background.colorRef()), Int.SIZE_BYTES)
    dwm.DwmSetWindowAttribute(handle, TextColor, IntByReference(ReseamDarkColors.foreground.colorRef()), Int.SIZE_BYTES)
}

private fun Color.colorRef(): Int {
    val rgb = toArgb()
    return ((rgb and 0xFF) shl 16) or (rgb and 0xFF00) or ((rgb shr 16) and 0xFF)
}
