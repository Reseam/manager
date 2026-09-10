package app.reseam.manager.platform

fun applyDisplayScale() {
    if (System.getProperty("os.name") != "Linux" || System.getProperty("sun.java2d.uiScale") != null || System.getenv("GDK_SCALE") != null) return
    val dpi = runCatching {
        ProcessBuilder("xrdb", "-query").redirectErrorStream(true).start().inputStream.bufferedReader().useLines { lines ->
            lines.firstNotNullOfOrNull { it.substringAfter("Xft.dpi:", "").trim().toDoubleOrNull() }
        }
    }.getOrNull() ?: return
    if (dpi > 96) System.setProperty("sun.java2d.uiScale", (dpi / 96).toString())
}
