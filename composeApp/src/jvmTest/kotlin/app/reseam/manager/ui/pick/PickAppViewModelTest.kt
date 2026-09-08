package app.reseam.manager.ui.pick

import app.reseam.manager.AppGraph
import app.reseam.manager.platform.ArtifactAction
import app.reseam.manager.platform.DesktopApkPresentationReader
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PickAppViewModelTest {
    @Test
    fun pickerFailuresAndCancellationReleaseTheBusyStateAndAllowRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val directory = createTempDirectory("reseam-picker-test").toFile()
        val artifactAction = object : ArtifactAction {
            override val label = "Open"
            override suspend fun run(apk: PlatformFile) = Unit
        }
        val graph = AppGraph(PlatformFile(directory), PlatformFile(directory), null, artifactAction, DesktopApkPresentationReader)
        try {
            val model = PickAppViewModel(graph)
            // Android launch failure is synchronous.
            model.pickFile { error("No document picker") }
            runCurrent()
            assertFalse(model.state.value.pickingFile)
            assertEquals("No document picker", graph.notices.notice.value?.message)

            // A desktop dialog may fail after its launch call returns.
            model.pickFile { }
            assertTrue(model.state.value.pickingFile)
            model.pickFile { error("Must not launch a second dialog") }
            model.onFilePicked(Result.failure(IllegalStateException("Dialog failed")))
            runCurrent()
            assertFalse(model.state.value.pickingFile)
            assertEquals("Dialog failed", graph.notices.notice.value?.message)

            graph.notices.notice.value?.let(graph.notices::dismiss)
            model.pickFile { }
            model.onFilePicked(Result.success(null))
            runCurrent()
            assertFalse(model.state.value.pickingFile)
            assertNull(graph.notices.notice.value)
            assertNull(model.state.value.selected)

            val notAnApk = directory.resolve("chosen.apkm").apply { writeText("picked content") }
            model.pickFile { }
            model.onFilePicked(Result.success(PlatformFile(notAnApk)))
            model.state.first { !it.pickingFile }
            assertNull(model.state.value.selected)
            assertTrue(graph.notices.notice.value?.message.orEmpty().isNotEmpty())
        } finally {
            graph.scope.cancel()
            Dispatchers.resetMain()
            directory.deleteRecursively()
        }
    }
}
