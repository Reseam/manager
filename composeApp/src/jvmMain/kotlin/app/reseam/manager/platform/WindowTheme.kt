package app.reseam.manager.platform

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.reseam.manager.ui.theme.DarkColors
import app.reseam.manager.ui.theme.LightColors
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

fun applyWindowTheme(window: Window, dark: Boolean) {
    if (DesktopOperatingSystem.current != DesktopOperatingSystem.Windows) return
    val dwm = Native.load("dwmapi", DwmApi::class.java)
    val handle = Native.getWindowPointer(window)
    val colors = if (dark) DarkColors else LightColors
    dwm.DwmSetWindowAttribute(handle, UseImmersiveDarkMode, IntByReference(if (dark) 1 else 0), Int.SIZE_BYTES)
    // Caption and text colors are available from Windows 11. Older Windows ignores these attributes.
    dwm.DwmSetWindowAttribute(handle, CaptionColor, IntByReference(colors.surface.colorRef()), Int.SIZE_BYTES)
    dwm.DwmSetWindowAttribute(handle, TextColor, IntByReference(colors.onSurface.colorRef()), Int.SIZE_BYTES)
}

private fun Color.colorRef(): Int {
    val rgb = toArgb()
    return ((rgb and 0xFF) shl 16) or (rgb and 0xFF00) or ((rgb shr 16) and 0xFF)
}
