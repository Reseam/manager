package app.reseam.manager.platform

enum class DesktopOperatingSystem(val browserPlatform: String) {
    Linux("X11; Linux x86_64"),
    Windows("Windows NT 10.0; Win64; x64"),
    MacOS("Macintosh; Intel Mac OS X 10.15"),
    ;

    companion object {
        val current: DesktopOperatingSystem = System.getProperty("os.name").let { name ->
            when {
                name.startsWith("Windows") -> Windows
                name == "Linux" -> Linux
                name.startsWith("Mac") || name.startsWith("Darwin") -> MacOS
                else -> error("Unsupported desktop operating system: $name")
            }
        }
    }
}
