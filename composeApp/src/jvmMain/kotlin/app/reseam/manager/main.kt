package app.reseam.manager

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.reseam.manager.platform.DesktopApkPresentationReader
import app.reseam.manager.platform.RevealInFolder
import app.reseam.manager.platform.applyDisplayScale
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.filesDir
import java.awt.Dimension

fun main() {
    applyDisplayScale()
    FileKit.init(appId = "app.reseam.manager")
    val graph = AppGraph(
        dataDirectory = FileKit.filesDir,
        cacheDirectory = FileKit.cacheDir,
        installedApps = null,
        artifactAction = RevealInFolder,
        presentation = DesktopApkPresentationReader,
    )
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Reseam Manager",
            state = rememberWindowState(size = DpSize(1160.dp, 800.dp)),
        ) {
            window.minimumSize = Dimension(420, 640)
            ReseamApp(graph, versionLabel = "Reseam Manager $ManagerVersion")
        }
    }
}
