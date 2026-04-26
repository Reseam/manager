package app.reseam.manager.ui.viewmodel

import app.reseam.manager.patcher.ApkMetadata
import app.reseam.manager.patcher.BundleMetadata
import app.reseam.manager.patcher.InspectRequest
import app.reseam.manager.patcher.InspectResponse
import app.reseam.manager.patcher.OptionKind
import app.reseam.manager.patcher.OptionMetadata
import app.reseam.manager.patcher.PatchArtifact
import app.reseam.manager.patcher.PatchMetadata
import app.reseam.manager.patcher.PatchOutcome
import app.reseam.manager.patcher.PatchOutput
import app.reseam.manager.patcher.PatchPhase
import app.reseam.manager.patcher.PatchRequest
import app.reseam.manager.patcher.PatchResult
import app.reseam.manager.patcher.PatchRunStatus
import app.reseam.manager.patcher.PatchStatus
import app.reseam.manager.patcher.ReseamBackend
import app.reseam.manager.patcher.ReseamCallResult
import app.reseam.manager.patcher.RunEvent
import app.reseam.manager.patcher.TrustStatus
import app.reseam.manager.ui.model.InstalledAppSummary
import app.reseam.manager.ui.model.LoadState
import app.reseam.manager.ui.model.navigation.ManagerRoute
import app.reseam.manager.ui.model.PatchRunConfig
import app.reseam.manager.ui.model.RunStatus
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.data.repository.InMemoryPatchedAppStore
import app.reseam.manager.data.repository.InMemoryBundleStore
import app.reseam.manager.data.repository.InMemoryPatchStore
import app.reseam.manager.domain.sources.BundleImporter
import app.reseam.manager.domain.sources.BundleImportResult
import app.reseam.manager.domain.sources.validateBundle
import app.reseam.manager.domain.sources.InstalledAppSource
import app.reseam.manager.domain.repository.BundleStore
import app.reseam.manager.domain.repository.PatchStore
import app.reseam.manager.domain.repository.PatchedAppStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ManagerViewModelTest {
    @Test
    fun continueFromInputsInspectsSelectedAppAndBuildsPatchEditor() = runTest {
        val backend = FakeBackend()
        val manager = manager(backend)

        manager.home.load()
        testScheduler.advanceUntilIdle()
        manager.home.startNewPatch()
        manager.inputs.selectInstalledApp("instagram")
        manager.inputs.continueToPatches()
        testScheduler.advanceUntilIdle()

        val state = manager.state
        assertEquals(ManagerRoute.Patches, state.route)
        assertIs<LoadState.Loaded<InspectResponse>>(state.flow.inspect)
        assertEquals("/apps/instagram/base.apk", backend.lastInspectRequest?.apkPath)
        assertEquals(2, state.flow.editor.patches.size)
        assertEquals(1, state.flow.editor.activeCount)
    }

    @Test
    fun runPatchAppliesEventsAndStoresPatchedApp() = runTest {
        val patchedStore = InMemoryPatchedAppStore()
        val backend = FakeBackend()
        val manager = manager(backend, patchedStore)

        manager.home.load()
        testScheduler.advanceUntilIdle()
        manager.home.startNewPatch()
        manager.inputs.selectInstalledApp("instagram")
        manager.inputs.continueToPatches()
        testScheduler.advanceUntilIdle()

        manager.patches.togglePatch("Download media", true)
        manager.run.run(PatchRunConfig(output = PatchOutput.SingleFile("/out/instagram.apk")))
        testScheduler.advanceUntilIdle()

        val state = manager.state
        assertEquals(ManagerRoute.Run, state.route)
        assertEquals(RunStatus.Finished, state.flow.run.status)
        assertEquals(100, state.flow.run.progressPercent)
        assertEquals("/out/instagram.apk", backend.lastPatchRequest?.output.let { (it as PatchOutput.SingleFile).path })
        assertTrue(backend.lastPatchRequest?.selection?.enable.orEmpty().contains("Download media"))
        assertNotNull(state.flow.run.artifact)
        assertEquals(1, patchedStore.list().size)
    }

    @Test
    fun navigationUsesTypedBackStack() = runTest {
        val manager = manager(FakeBackend())

        manager.home.load()
        testScheduler.advanceUntilIdle()
        manager.home.startNewPatch()
        manager.navigation.back()

        assertEquals(ManagerRoute.Home, manager.state.route)

        manager.home.openSettings()
        manager.navigation.back()

        assertEquals(ManagerRoute.Home, manager.state.route)
    }

    @Test
    fun loadBootstrapsOfficialBundleWhenStoreIsEmpty() = runTest {
        val bundleStore = InMemoryBundleStore()
        val manager = manager(
            backend = FakeBackend(),
            bundleStore = bundleStore,
            bundleImporter = FakeBundleImporter(),
        )

        manager.home.load()
        testScheduler.advanceUntilIdle()

        val bundle = manager.state.bundles.installed.single()
        assertEquals("reseam-patches", bundle.id)
        assertEquals("/bundles/reseam-patches.reseam", bundle.path)
        assertTrue(bundle.official)
        assertTrue(bundle.trusted)
        assertEquals(1, bundleStore.list().size)
    }

    @Test
    fun bundleValidationUsesPatcherMetadata() = runTest {
        val backend = FakeBackend()

        val result = backend.validateBundle("/bundles/reseam-patches.reseam", "https://reseam.app/bundle")
        val bundle = result.summary

        assertEquals("reseam-patches", bundle.name)
        assertEquals("Official", bundle.description)
        assertEquals("https://reseam.app/bundle", bundle.source)
        assertEquals("/bundles/reseam-patches.reseam", bundle.path)
        assertEquals("00", bundle.signerPublicKeyHex)
        assertEquals(2, bundle.patchCount)
        assertTrue(bundle.trusted)
        assertEquals(2, result.patches.size)
    }

    private fun TestScope.manager(
        backend: FakeBackend,
        patchedStore: PatchedAppStore = InMemoryPatchedAppStore(),
        bundleStore: BundleStore = InMemoryBundleStore(),
        patchStore: PatchStore = InMemoryPatchStore(),
        bundleImporter: BundleImporter? = null,
    ): ManagerViewModel =
        ManagerViewModel(
            backend = backend,
            installedApps = FakeInstalledAppSource(),
            patchedApps = patchedStore,
            bundleStore = bundleStore,
            patchStore = patchStore,
            bundleImporter = bundleImporter,
            scope = this,
        )
}

private class FakeBundleImporter : BundleImporter {
    override suspend fun syncOfficial(apiBaseUrl: String, currentVersion: String?): BundleImportResult? {
        if (currentVersion == "0.1.0") return null
        return BundleImportResult(
            summary = BundleSummary(
                id = "reseam-patches",
                name = "reseam-patches",
                description = "Official",
                source = "https://api.reseam.app/patches/v0.1.0/reseam-patches.reseam",
                official = true,
                trusted = true,
                patchCount = 2,
                version = "0.1.0",
                signerPublicKeyHex = "00",
                signerFingerprint = "00",
                path = "/bundles/reseam-patches.reseam",
            ),
            patches = emptyList(),
        )
    }

    override suspend fun importFromUrl(url: String): BundleImportResult = error("not used")

    override suspend fun importFromFile(path: String): BundleImportResult = error("not used")
}

private class FakeInstalledAppSource : InstalledAppSource {
    override suspend fun installedApps(): List<InstalledAppSummary> =
        listOf(
            InstalledAppSummary(
                id = "instagram",
                name = "Instagram",
                packageName = "com.instagram.android",
                versionName = "419.0.0.49.71",
                apkPath = "/apps/instagram/base.apk",
                compatiblePatchCount = 2,
            ),
        )
}

private class FakeBackend : ReseamBackend {
    var lastInspectRequest: InspectRequest? = null
    var lastPatchRequest: PatchRequest? = null

    override suspend fun inspectApk(
        apkPath: String,
        splitPaths: List<String>,
    ): ReseamCallResult<ApkMetadata> =
        ReseamCallResult.Failure("not used")

    override suspend fun inspect(request: InspectRequest): ReseamCallResult<InspectResponse> {
        lastInspectRequest = request
        return ReseamCallResult.Success(inspectResponse())
    }

    override suspend fun patch(
        request: PatchRequest,
        onEvent: (RunEvent) -> Unit,
    ): ReseamCallResult<PatchOutcome> {
        lastPatchRequest = request
        onEvent(RunEvent.PatchStarted("Instagram settings"))
        onEvent(RunEvent.PatchFinished("Instagram settings", PatchRunStatus.Applied))
        onEvent(RunEvent.PatchStarted("Download media"))
        onEvent(RunEvent.PatchFinished("Download media", PatchRunStatus.Applied))
        return ReseamCallResult.Success(
            PatchOutcome(
                results = listOf(
                    PatchResult("Instagram settings", PatchStatus.Applied),
                    PatchResult("Download media", PatchStatus.Applied),
                ),
                artifact = PatchArtifact(app.reseam.manager.patcher.ArtifactKind.Apk, "/out/instagram.apk"),
            ),
        )
    }

    private fun inspectResponse(): InspectResponse =
        InspectResponse(
            apk = ApkMetadata(
                packageName = "com.instagram.android",
                versionName = "419.0.0.49.71",
            ),
            bundles = listOf(
                BundleMetadata(
                    fileName = "reseam-patches.bundle",
                    name = "reseam-patches",
                    author = "Reseam",
                    description = "Official",
                    signerPublicKeyHex = "00",
                    signerFingerprint = "00",
                    trustStatus = TrustStatus.Trusted,
                ),
            ),
            patches = listOf(
                PatchMetadata(
                    sourceBundle = "reseam-patches",
                    name = "Instagram settings",
                    description = "Settings entry point",
                    enabledByDefault = true,
                    compatibleWith = emptyList(),
                    options = emptyList(),
                    isCompatible = true,
                ),
                PatchMetadata(
                    sourceBundle = "reseam-patches",
                    name = "Download media",
                    description = "Adds downloads",
                    enabledByDefault = false,
                    compatibleWith = emptyList(),
                    options = listOf(
                        OptionMetadata(
                            key = "folder",
                            title = "Folder",
                            description = "Save folder",
                            optionType = OptionKind.Path,
                            defaultValue = app.reseam.manager.patcher.InputOptionValue.PathValue("/downloads"),
                            required = false,
                        ),
                    ),
                    isCompatible = true,
                ),
            ),
        )
}
