package app.reseam.manager.data

import app.reseam.sdk.Problem
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Runs the official sync and third-party import against real signed bundles through the engine.
 * Needs `-PreseamTestBundle=<official .reseam>`, `-PreseamTestOtherBundle=<same payload signed by another key>`,
 * and `-PreseamTestStaleBundle=<official bundle packed by an older engine line>`.
 */
class BundleRepositoryTest {
    private val officialBundle = File(System.getProperty("reseamTestBundle").also(::assumeNotNull))
    private val otherBundle = File(System.getProperty("reseamTestOtherBundle").also(::assumeNotNull))
    private val staleBundle = File(System.getProperty("reseamTestStaleBundle").also(::assumeNotNull))
    private val otherKey = signerOf(otherBundle)

    private suspend fun repository(): BundleRepository {
        val directory = PlatformFile(createTempDirectory("reseam-test").toFile())
        return BundleRepository(JsonStore(directory, "bundles.json", BundleLibrary.serializer(), BundleLibrary()), directory / "bundles").apply { load() }
    }

    private fun BundleRepository.patchesOf(bundle: Bundle) = checkNotNull(patches.value)[bundle.id].orEmpty()

    @Test
    fun selfHostedApiTrustsOnFirstUseAndBlocksOnKeyChange() = runTest {
        val repository = repository()
        TestApi(OfficialSignerKey, officialBundle, "1").use { api ->
            val staged = repository.sync(api.baseUrl, SyncScope.All).prompt
            assertEquals(TrustPrompt.NewApiSigner, staged?.prompt)
            assertSame(staged, repository.pending.value)
            assertTrue(repository.installed().isEmpty())

            repository.decide(trust = true)
            val official = repository.installed().single()
            assertTrue(official.official)
            assertEquals(OfficialSignerKey, official.id)
            assertEquals("1", official.version)
            assertTrue(repository.patchesOf(official).isNotEmpty())
            assertNull(repository.pending.value)

            assertNull(repository.sync(api.baseUrl, SyncScope.All).prompt)
            assertEquals(listOf(OfficialSignerKey), repository.trust().keys)
        }
        val previous = repository.installed().single()
        TestApi(otherKey, otherBundle, "2").use { api ->
            val changed = repository.sync(api.baseUrl, SyncScope.All).prompt
            assertEquals(TrustPrompt.ChangedSigner(OfficialSignerKey), changed?.prompt)
            assertEquals(previous.copy(checkedAtEpochMs = null), repository.installed().single().copy(checkedAtEpochMs = null))

            repository.decide(trust = true)
            val replaced = repository.installed().single()
            assertTrue(replaced.official)
            assertEquals(otherKey, replaced.id)
            assertEquals("2", replaced.version)
            assertTrue(repository.patchesOf(replaced).isNotEmpty())
            assertFalse(File(previous.path).exists())
        }
    }

    @Test
    fun declinedSignerLeavesNothingBehind() = runTest {
        val repository = repository()
        TestApi(otherKey, otherBundle, "1").use { api ->
            val staged = assertNotNull(repository.sync(api.baseUrl, SyncScope.All).prompt)
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
            val failure = repository.sync(api.baseUrl, SyncScope.All).failures.single()
            assertEquals("Official patches", failure.bundle)
            assertEquals("reseam-patches is not signed by the key its publisher lists", failure.error.message)
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
            assertTrue(repository.patchesOf(bundle).isNotEmpty())

            val again = repository.stageDownload(api.bundleUrl)
            assertNull(again.prompt)
            assertTrue(again.patches.isNotEmpty())
            assertNull(repository.offer(again))
            assertEquals(1, repository.installed().size)
        }
    }

    @Test
    fun bundlesTheEngineCannotLoadAreUninstalled() = runTest {
        val directory = createTempDirectory("reseam-test").toFile()
        val installed = directory.resolve("bundles/$OfficialSignerKey.reseam").apply { parentFile.mkdirs() }
        staleBundle.copyTo(installed)
        val store = JsonStore(PlatformFile(directory), "bundles.json", BundleLibrary.serializer(), BundleLibrary())
        store.update { BundleLibrary(listOf(Bundle(OfficialSignerKey, "reseam-patches", "Reseam", "", "1", official = true, origin = "test", path = installed.absolutePath))) }
        val repository = BundleRepository(store, PlatformFile(directory) / "bundles")

        val (bundle, problem) = repository.load().single()
        assertEquals(OfficialSignerKey, bundle.id)
        assertIs<Problem.BundleTooOld>(problem)
        assertTrue(repository.installed().isEmpty())
        assertEquals(emptyMap(), repository.patches.value)
        assertFalse(installed.exists())
    }

    @Test
    fun indexBundleUpdatesFromItsSignerOnlyWhenDue() = runTest {
        val repository = repository()
        TestApi(otherKey, otherBundle, "1").use { api ->
            val staged = repository.stageDownload(api.indexUrl)
            assertEquals(TrustPrompt.UnknownSigner, staged.prompt)
            assertSame(staged, repository.offer(staged))
            repository.decide(trust = true)
            val subscribed = repository.installed().single()
            assertEquals(api.indexUrl, subscribed.index)
            assertEquals("1", subscribed.version)
            assertTrue(subscribed.followsUpdates)
            assertTrue(repository.patchesOf(subscribed).isNotEmpty())

            api.version = "2"
            val due = repository.sync(api.missingApiUrl, SyncScope.Due)
            assertNull(due.prompt)
            assertEquals(listOf("Official patches"), due.failures.map { it.bundle })
            assertEquals("1", repository.installed().single().version)

            val forced = repository.sync(api.missingApiUrl, SyncScope.All)
            assertNull(forced.prompt)
            assertNull(repository.pending.value)
            val updated = repository.installed().single()
            assertEquals("2", updated.version)
            assertEquals(api.indexUrl, updated.index)
            assertTrue(repository.patchesOf(updated).isNotEmpty())
        }
    }

    @Test
    fun indexSignerChangeNeedsConfirmationAndReplacesTheBundle() = runTest {
        val repository = repository()
        TestApi(otherKey, otherBundle, "1").use { api ->
            repository.offer(repository.stageDownload(api.indexUrl))
            repository.decide(trust = true)
            val previous = repository.installed().single()

            api.servedKey = OfficialSignerKey
            api.bundle = officialBundle
            api.version = "2"
            val changed = assertNotNull(repository.sync(api.missingApiUrl, SyncScope.All).prompt)
            assertEquals(TrustPrompt.ChangedSigner(otherKey), changed.prompt)
            assertEquals(previous.copy(checkedAtEpochMs = null), repository.installed().single().copy(checkedAtEpochMs = null))

            repository.decide(trust = true)
            val replaced = repository.installed().single()
            assertEquals(OfficialSignerKey, replaced.id)
            assertFalse(replaced.official)
            assertEquals(api.indexUrl, replaced.index)
            assertEquals("2", replaced.version)
            assertFalse(File(previous.path).exists())
        }
    }

    @Test
    fun anIndexBundleMustBeSignedByTheKeyItsIndexNames() = runTest {
        val repository = repository()
        TestApi(OfficialSignerKey, otherBundle, "1").use { api ->
            val error = assertFailsWith<IllegalStateException> { repository.stageDownload(api.indexUrl) }
            assertEquals("reseam-patches is not signed by the key its publisher lists", error.message)
            assertTrue(repository.installed().isEmpty())
        }
    }

    @Test
    fun aUrlThatIsNeitherABundleNorAnIndexIsRejected() = runTest {
        val repository = repository()
        TestApi(otherKey, otherBundle, "1").use { api ->
            val url = api.baseUrl + "/patches/keys"
            val error = assertFailsWith<IllegalArgumentException> { repository.stageDownload(url) }
            assertEquals("$url is neither a bundle nor a bundle's patches.json", error.message)
        }
    }

    @Test
    fun aTooOldIndexBundleStaysForTheNextSync() = runTest {
        val directory = createTempDirectory("reseam-test").toFile()
        val installed = directory.resolve("bundles/$OfficialSignerKey.reseam").apply { parentFile.mkdirs() }
        staleBundle.copyTo(installed)
        val store = JsonStore(PlatformFile(directory), "bundles.json", BundleLibrary.serializer(), BundleLibrary())
        val bundle = Bundle(OfficialSignerKey, "reseam-patches", "Reseam", "", "1", official = false, origin = "test", path = installed.absolutePath, index = "https://example.invalid/patches.json", checkedAtEpochMs = 1)
        store.update { BundleLibrary(listOf(bundle)) }
        val repository = BundleRepository(store, PlatformFile(directory) / "bundles")

        assertTrue(repository.load().isEmpty())
        assertEquals(bundle.copy(checkedAtEpochMs = null), repository.installed().single())
        assertEquals(emptyMap(), repository.patches.value)
        assertTrue(installed.exists())
    }

    private fun signerOf(bundle: File): String =
        java.util.zip.ZipFile(bundle).use { zip -> zip.getInputStream(zip.getEntry("manifest.pubkey")).readBytes().joinToString("") { "%02x".format(it) } }
}
