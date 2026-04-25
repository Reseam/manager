package app.reseam.manager.data.platform

import java.io.File

internal fun desktopDataFile(name: String): File =
    File(System.getProperty("user.home"), ".reseam/manager/$name")
