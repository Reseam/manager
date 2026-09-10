package app.reseam.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import app.reseam.manager.AppGraph
import app.reseam.manager.LocalAppGraph
import app.reseam.manager.data.SigningKeyInfo
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.DesktopApkPresentationReader
import app.reseam.manager.platform.Permission
import app.reseam.manager.platform.Permissions
import app.reseam.manager.sdk.PatchSelection
import app.reseam.manager.ui.bundles.AddBundleSheet
import app.reseam.manager.ui.nav.AppNavigation
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.settings.PasswordSheet
import app.reseam.manager.ui.settings.SigningKeySheet
import app.reseam.manager.ui.theme.ReseamTheme
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assume.assumeNotNull
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.Test

/**
 * Renders every screen at phone, tablet, and desktop widths into PNGs for eyeballing layouts; input
 * cannot be faked into the real window on Wayland. Needs `-PreseamScreenshotData=<copy of a data dir>`
 * and `-PreseamScreenshotOut=<dir>`; the Patches and Run shots need that library's source APK on disk.
 */
class ScreenshotTest {
    private val data = File(System.getProperty("reseamScreenshotData").also(::assumeNotNull))
    private val out = File(System.getProperty("reseamScreenshotOut").also(::assumeNotNull)).apply { mkdirs() }
    private val widths = listOf("compact" to 412, "medium" to 720, "expanded" to 1200)

    private val graph = AppGraph(
        dataDirectory = PlatformFile(data),
        cacheDirectory = PlatformFile(data.resolve("cache")),
        installedApps = null,
        artifactAction = object : ArtifactAction {
            override val label = "Show in folder"
            override suspend fun run(apk: PlatformFile) = Unit
        },
        presentation = DesktopApkPresentationReader,
    )
    private val permissions = object : Permissions {
        override val granted = setOf(Permission.InstallApps)
        override fun request(permission: Permission) = Unit
    }

    private val target = graph.patchedAppsSnapshot()
        .firstOrNull { app -> app.sourceApkPath?.let { File(it).exists() } == true }
        ?.let { PatchTarget(it.name, it.packageName, it.versionName, checkNotNull(it.sourceApkPath), it.sourceSplitPaths, it.iconPath) }

    @Test
    fun screens() {
        val bundle = graph.bundles.installed().first()
        val library = graph.patchedAppsSnapshot()
        shoot("home", listOf(Route.Home))
        library.firstOrNull()?.let { shoot("app-detail", listOf(Route.Home, Route.AppDetail(it.packageName))) }
        shoot("pick", listOf(Route.PickApp))
        shoot("bundles", listOf(Route.Bundles))
        shoot("bundle-detail", listOf(Route.Bundles, Route.BundleDetail(bundle.id)))
        shoot("settings", listOf(Route.Settings))
        shoot("permissions", listOf(Route.Permissions))
        target?.let { shoot("patches", listOf(Route.Patches(it)), seconds = 8.0) }
        sheet("sheet-signing") { SigningKeySheet(SigningKeyInfo("AB:CD:".repeat(16).dropLast(1)), onDismiss = {}, onExport = {}, onImport = {}, onReset = {}) }
        sheet("sheet-password") { PasswordSheet("Keystore password", "Enter the password protecting reseam.p12.", "Import", onDismiss = {}, onConfirm = {}) }
        sheet("sheet-add-bundle") { AddBundleSheet(onDismiss = {}, onUrl = {}, onFile = {}) }
    }

    @Test
    fun run() {
        val target = target.also(::assumeNotNull)!!
        val queue = graph.bundles.installed().flatMap { it.patches }.filter { it.supports(target.packageName!!) && it.enabledByDefault }.map { it.id }
        val key = data.resolve("signing/reseam.pk8")
        shoot("run", listOf(Route.Run(target, PatchSelection(), queue, graph.bundles.paths())), seconds = 90.0, settled = { key.exists() })
        check(key.exists()) { "the run did not create the manager signing key" }
    }

    private fun shoot(name: String, stack: List<Route>, seconds: Double = 4.0, settled: () -> Boolean = { false }) {
        widths.forEach { (label, width) ->
            render("$name-$label", width, seconds, settled) {
                AppNavigation("Reseam Manager 0.0.0", permissions, stack, notices = {})
            }
        }
    }

    private fun sheet(name: String, content: @Composable () -> Unit) {
        listOf(widths.first(), widths.last()).forEach { (label, width) -> render("$name-$label", width, 2.0, { false }, content) }
    }

    /**
     * Composition and lifecycle run on the Swing thread, which is also the view models' main dispatcher,
     * so frames are rendered one at a time from here to let their coroutines run in between.
     */
    private fun render(name: String, width: Int, seconds: Double, settled: () -> Boolean, content: @Composable () -> Unit) {
        val owner = onUi { SceneOwner() }
        val scene = onUi {
            ImageComposeScene(width = width, height = 900, density = Density(1f)).apply {
                setContent {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner, LocalAppGraph provides graph) {
                        ReseamTheme { Box(Modifier.fillMaxSize().background(ReseamTheme.colors.background)) { content() } }
                    }
                }
            }
        }
        try {
            var image = onUi { scene.render(0) }
            val frames = (seconds * 30).toInt()
            var frame = 0
            var graceFrames = -1
            while (frame < frames && graceFrames != 0) {
                Thread.sleep(33)
                frame++
                image = onUi { scene.render(frame * 33_000_000L) }
                if (graceFrames < 0 && settled()) graceFrames = 60
                if (graceFrames > 0) graceFrames--
            }
            out.resolve("$name.png").writeBytes(checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
        } finally {
            onUi {
                scene.close()
                owner.viewModelStore.clear()
            }
        }
    }

    private fun <T> onUi(block: () -> T): T {
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }

    private class SceneOwner : LifecycleOwner, ViewModelStoreOwner {
        override val lifecycle = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val viewModelStore = ViewModelStore()
    }
}

private fun AppGraph.patchedAppsSnapshot() = runBlocking { patchedApps.apps.first() }
