package app.reseam.manager.data

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeNotNull
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Runs the official sync and third-party import against real signed bundles through the engine.
 * Needs `-PreseamTestBundle=<official .reseam>` and `-PreseamTestOtherBundle=<same payload signed by another key>`.
 */
class BundleRepositoryTest {
    private val officialBundle = File(System.getProperty("reseamTestBundle").also(::assumeNotNull))
    private val otherBundle = File(System.getProperty("reseamTestOtherBundle").also(::assumeNotNull))
    private val otherKey = signerOf(otherBundle)

    private fun repository(): BundleRepository {
        val directory = PlatformFile(createTempDirectory("reseam-test").toFile())
        return BundleRepository(JsonStore(directory, "bundles.json", BundleLibrary.serializer(), BundleLibrary()), directory / "bundles")
    }

    @Test
    fun selfHostedApiTrustsOnFirstUseAndBlocksOnKeyChange() = runTest {
        val repository = repository()
        TestApi(OfficialSignerKey, officialBundle, "1").use { api ->
            val staged = repository.syncOfficial(api.baseUrl, force = true)
            assertEquals(TrustPrompt.NewApiSigner, staged?.prompt)
            assertSame(staged, repository.pending.value)
            assertTrue(repository.installed().isEmpty())

            repository.decide(trust = true)
            val official = repository.installed().single()
            assertTrue(official.official)
            assertEquals(OfficialSignerKey, official.id)
            assertEquals("1", official.version)
            assertTrue(official.patches.isNotEmpty())
            assertNull(repository.pending.value)

            assertNull(repository.syncOfficial(api.baseUrl, force = true))
            assertEquals(listOf(OfficialSignerKey), repository.trust().keys)
        }
        val previous = repository.installed().single()
        TestApi(otherKey, otherBundle, "2").use { api ->
            val changed = repository.syncOfficial(api.baseUrl, force = true)
            assertEquals(TrustPrompt.ChangedApiSigner(OfficialSignerKey), changed?.prompt)
            assertEquals(previous, repository.installed().single())

            repository.decide(trust = true)
            val replaced = repository.installed().single()
            assertTrue(replaced.official)
            assertEquals(otherKey, replaced.id)
            assertEquals("2", replaced.version)
            assertTrue(replaced.patches.isNotEmpty())
            assertFalse(File(previous.path).exists())
        }
    }

    @Test
    fun declinedSignerLeavesNothingBehind() = runTest {
        val repository = repository()
        TestApi(otherKey, otherBundle, "1").use { api ->
            val staged = assertNotNull(repository.syncOfficial(api.baseUrl, force = true))
            repository.decide(trust = false)
            assertTrue(repository.installed().isEmpty())
            assertNull(repository.pending.value)
            assertFalse(File(staged.file.toString()).exists())
        }
    }

    @Test
    fun theServedKeyMustSignTheBundle() = runTest {
        val repository = repository()
        TestApi(otherKey, officialBundle, "1").use { api ->
            val error = assertFailsWith<IllegalStateException> { repository.syncOfficial(api.baseUrl, force = true) }
            assertEquals("The official bundle is not signed by the key the API published", error.message)
            assertTrue(repository.installed().isEmpty())
        }
    }

    @Test
    fun thirdPartyBundleIsConfirmedOnceThenTrusted() = runTest {
        val repository = repository()
        TestApi(otherKey, otherBundle, "1").use { api ->
            val staged = repository.stageDownload(api.bundleUrl)
            assertEquals(TrustPrompt.UnknownSigner, staged.prompt)
            assertTrue(staged.patches.isEmpty())
            assertSame(staged, repository.offer(staged))

            repository.decide(trust = true)
            val bundle = repository.installed().single()
            assertFalse(bundle.official)
            assertEquals(otherKey, bundle.id)
            assertTrue(bundle.patches.isNotEmpty())

            val again = repository.stageDownload(api.bundleUrl)
            assertNull(again.prompt)
            assertTrue(again.patches.isNotEmpty())
            assertNull(repository.offer(again))
            assertEquals(1, repository.installed().size)
        }
    }

    private fun signerOf(bundle: File): String =
        java.util.zip.ZipFile(bundle).use { zip -> zip.getInputStream(zip.getEntry("manifest.pubkey")).readBytes().joinToString("") { "%02x".format(it) } }
}
