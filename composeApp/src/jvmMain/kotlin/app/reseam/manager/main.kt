package app.reseam.manager

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.reseam.manager.platform.DesktopApkPresentationReader
import app.reseam.manager.platform.RevealInFolder
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.filesDir

fun main() {
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
            state = rememberWindowState(size = DpSize(480.dp, 860.dp)),
        ) {
            ReseamApp(graph, versionLabel = "Reseam Manager $ManagerVersion")
        }
    }
}
