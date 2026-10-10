package app.reseam.manager

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.reseam.manager.platform.DesktopApkPresentationReader
import app.reseam.manager.platform.DesktopDirectories
import app.reseam.manager.platform.DesktopSourceSession
import app.reseam.manager.platform.RevealInFolder
import app.reseam.manager.platform.applyDisplayScale
import app.reseam.manager.platform.applyWindowTheme
import app.reseam.manager.platform.rememberSystemDarkTheme
import app.reseam.manager.platform.runSingleInstance
import app.reseam.manager.resources.*
import app.reseam.manager.ui.ReseamApp
import app.reseam.manager.ui.components.BrandLogo
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.createDirectories
import java.awt.Dimension
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource

fun main() {
    applyDisplayScale()
    FileKit.init(appId = "app.reseam.manager")
    val directories = DesktopDirectories()
    directories.data.createDirectories()
    runSingleInstance(directories.data.file.toPath()) { activations -> runManager(directories, activations) }
}

private fun runManager(directories: DesktopDirectories, activations: Flow<Unit>) {
    directories.openLog()
    directories.prepareTemporaryDirectory()
    val graph = AppGraph(
        dataDirectory = directories.data,
        cacheDirectory = directories.cache,
        installedApps = null,
        artifactAction = RevealInFolder,
        mounter = null,
        presentation = DesktopApkPresentationReader,
        sourceSession = DesktopSourceSession,
        device = null,
        backgroundRun = null,
    )
    application {
        val state = rememberWindowState(size = DpSize(1160.dp, 800.dp))
        Window(
            onCloseRequest = ::exitApplication,
            title = stringResource(Res.string.app_name),
            icon = rememberVectorPainter(BrandLogo),
            state = state,
        ) {
            window.minimumSize = Dimension(420, 640)
            val dark = rememberSystemDarkTheme()
            LaunchedEffect(window, dark) { applyWindowTheme(window, dark) }
            LaunchedEffect(Unit) {
                activations.collect {
                    state.isMinimized = false
                    window.toFront()
                }
            }
            ReseamApp(graph, dark)
        }
    }
}
