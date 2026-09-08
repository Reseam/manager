package app.reseam.manager.data

import app.reseam.manager.platform.DesktopApkPresentationReader
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeNotNull
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Reads a real app through the native SDK. Needs `-PreseamTestApk=<path>` and `-PreseamTestApkLabel=<launcher name>`. */
class AppIdentityReaderTest {
    private val apk = File(System.getProperty("reseamTestApk").also(::assumeNotNull))
    private val label = System.getProperty("reseamTestApkLabel").also(::assumeNotNull)

    @Test
    fun identityComesFromTheArchiveNotTheFileName() = runTest {
        val directory = createTempDirectory("reseam-identity-test").toFile()
        try {
            val identity = AppIdentityReader(PlatformFile(directory), DesktopApkPresentationReader).read(apk.absolutePath)
            assertEquals(label, identity.name)
            assertNotNull(identity.packageName)
            assertNotNull(identity.versionName)
            val icon = File(assertNotNull(identity.iconPath)).readBytes()
            assertTrue(icon.size > 8 && icon[1] == 'P'.code.toByte() && icon[2] == 'N'.code.toByte() && icon[3] == 'G'.code.toByte())
        } finally {
            directory.deleteRecursively()
        }
    }
}
