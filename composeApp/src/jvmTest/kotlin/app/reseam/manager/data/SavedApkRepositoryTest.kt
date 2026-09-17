package app.reseam.manager.data

import app.reseam.manager.platform.DesktopApkPresentationReader
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeNotNull
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedApkRepositoryTest {
    private fun <T> withRepository(block: suspend (SavedApkRepository, File) -> T) = runTest {
        val directory = createTempDirectory("reseam-saved-test").toFile()
        try {
            val repository = SavedApkRepository(
                JsonStore(PlatformFile(directory), "saved-apks.json", SavedApkLibrary.serializer(), SavedApkLibrary()),
                PlatformFile(directory.resolve("saved-apks")),
                AppIdentityReader(PlatformFile(directory.resolve("icons")), DesktopApkPresentationReader),
            )
            block(repository, directory.resolve("saved-apks"))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun aFileThatIsNotAnApkLeavesNothingBehind() = withRepository { saved, directory ->
        assertFails { saved.save("apk", SavedApkOrigin.File) { File(it.absolutePath()).writeText("not an app") } }
        assertEquals(emptyList(), saved.apks.first())
        assertEquals(emptyList(), directory.resolve("staging").listFiles().orEmpty().toList())
    }

    @Test
    fun clearingStagingDropsHalfWrittenFiles() = withRepository { saved, directory ->
        val partial = directory.resolve("staging").apply { mkdirs() }.resolve("cut-short.apk").apply { writeText("partial") }
        saved.clearStaging()
        assertFalse(partial.exists())
    }

    /** Needs `-PreseamTestApk=<path>`. */
    @Test
    fun aSavedApkIsReusedReplacedByTheSameBuildAndDeletedWithItsEntry() = withRepository { saved, directory ->
        val apk = File(System.getProperty("reseamTestApk").also(::assumeNotNull))
        val build = SourceBuild("example", "YXBwLTEtMC1hbmRyb2lkLWFway1kb3dubG9hZA")
        val downloaded = saved.save("apk", SavedApkOrigin.Download, build) { apk.copyTo(File(it.absolutePath())) }
        assertTrue(File(downloaded.path).exists())
        assertEquals(directory.canonicalPath, File(downloaded.path).parentFile.canonicalPath)
        assertEquals(apk.length(), downloaded.sizeBytes)
        assertEquals(downloaded, saved.downloaded(build))

        val picked = saved.save("apk", SavedApkOrigin.File) { apk.copyTo(File(it.absolutePath())) }
        assertEquals(listOf(picked), saved.apks.first())
        assertFalse(File(downloaded.path).exists())

        saved.remove(picked.id)
        assertEquals(emptyList(), saved.apks.first())
        assertFalse(File(picked.path).exists())
    }
}
