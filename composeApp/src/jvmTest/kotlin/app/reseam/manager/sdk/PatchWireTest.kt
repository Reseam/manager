package app.reseam.manager.sdk

import app.reseam.manager.data.AppliedPatch
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.patches.PatchEditor
import app.reseam.manager.ui.run.RunState
import app.reseam.sdk.Compatibility
import app.reseam.sdk.CompatiblePackage
import app.reseam.sdk.InspectResponse
import app.reseam.sdk.LogEntry
import app.reseam.sdk.LogLevel
import app.reseam.sdk.PatchArtifact
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.PatchMetrics
import app.reseam.sdk.PatchOutcome
import app.reseam.sdk.PatchOutput
import app.reseam.sdk.PatchRequest
import app.reseam.sdk.PatchResult
import app.reseam.sdk.PatchSelection
import app.reseam.sdk.PatchSpec
import app.reseam.sdk.PatchStatus
import app.reseam.sdk.RunEvent
import app.reseam.sdk.Trust
import app.reseam.sdk.encodeSelection
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PatchWireTest {
    @Test
    fun automaticRequestUsesTheSdkDiscriminator() {
        val request = PatchRequest(splitPaths = emptyList(), trust = Trust(emptyList()), selection = PatchSelection(emptyList(), emptyList(), emptyMap()), apkPath = "app.xapk", bundlePaths = emptyList(), output = PatchOutput.Auto("com.example.reseamed"))
        val output = assertIs<PatchOutput.Auto>(request.output)
        assertEquals("com.example.reseamed", output.path)
    }

    @Test
    fun outcomeCarriesTheResolvedArtifactForBothOutputShapes() {
        for ((kind, path) in listOf("single_file" to "com.example.reseamed.apk", "split_dir" to "com.example.reseamed")) {
            val outcome = PatchOutcome(
                output = if (kind == "single_file") PatchArtifact.SingleFile(path) else PatchArtifact.SplitDir(path),
                results = emptyList(),
                metrics = PatchMetrics(1uL, null, null, null, null, null, emptyList(), null),
            )
            assertEquals(path, outcome.output.path)
            if (kind == "single_file") assertIs<PatchArtifact.SingleFile>(outcome.output)
            else assertIs<PatchArtifact.SplitDir>(outcome.output)
        }
    }

    @Test
    fun nativeSdkAcceptsAutomaticOutputAndRejectsAnInvalidContainerBeforeLoadingBundles() = runTest {
        val directory = createTempDirectory("reseam-wire-test").toFile()
        try {
            val input = directory.resolve("empty.apkm")
            ZipOutputStream(input.outputStream()).use { }
            val error = assertFailsWith<Exception> {
                ReseamSdk.patch(PatchRequest(splitPaths = emptyList(), trust = Trust(emptyList()), selection = PatchSelection(emptyList(), emptyList(), emptyMap()),
                    apkPath = input.absolutePath,
                    bundlePaths = emptyList(),
                    output = PatchOutput.Auto(directory.resolve("patched").absolutePath),
                )) { }
            }
            assertTrue(error.message.orEmpty().contains("archive contains no APK entries"), error.toString())
            assertEquals(listOf("empty.apkm"), directory.list()?.toList())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun theAppliedCountLeavesOutInternalsAndDependencies() {
        val state = RunState(
            results = listOf(
                PatchResult(hidden = false, requiredBy = emptyList(), logs = emptyList(), patch = "test/chosen", status = PatchStatus.Applied),
                PatchResult(hidden = false, logs = emptyList(), patch = "test/dependency", requiredBy = listOf("test/chosen"), status = PatchStatus.Applied),
                PatchResult(logs = emptyList(), patch = "test/internal", hidden = true, requiredBy = listOf("test/chosen"), status = PatchStatus.Applied),
                PatchResult(hidden = false, requiredBy = emptyList(), logs = emptyList(), patch = "test/skipped", status = PatchStatus.Skipped("not selected")),
            ),
        )
        assertEquals(1, state.applied)
    }

    @Test
    fun resultsAndEventsNameThePatchByReference() {
        val result = PatchResult(
            patch = "test/hide-ads", hidden = false, requiredBy = listOf("test/premium"),
            status = PatchStatus.Applied, logs = listOf(LogEntry(LogLevel.INFO, "test/hide-ads", "done")),
        )
        assertEquals("test/hide-ads", result.patch)
        assertEquals(listOf("test/premium"), result.requiredBy)
        assertEquals("test/hide-ads", result.logs.single().patch)
        val finished: RunEvent = RunEvent.PatchFinished("test/hide-ads", PatchStatus.Applied)
        assertEquals("test/hide-ads", assertIs<RunEvent.PatchFinished>(finished).patch)
    }

    @Test
    fun theSelectionSentToTheEngineUsesReferences() {
        val first = PatchMetadata(spec = PatchSpec(hidden = false, dependencies = emptyList(), options = emptyList(), bundle = "one", id = "hide-ads", name = "Hide Ads", description = "", enabledByDefault = true, compatibility = Compatibility.Universal))
        val second = first.copy(spec = first.spec.copy(bundle = "two", enabledByDefault = false))
        val editor = PatchEditor.from(InspectResponse(bundles = emptyList(), patches = listOf(first, second)), packageName = null, allowIncompatible = false)
        assertEquals(listOf("one/hide-ads", "two/hide-ads"), editor.rows.map { it.reference })
        val selection = Json.parseToJsonElement(encodeSelection(editor.selection())).jsonObject
        assertEquals(listOf("one/hide-ads"), selection.getValue("enable").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("two/hide-ads"), selection.getValue("disable").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("one/hide-ads"), editor.queue())
    }

    @Test
    fun runMetadataSurvivesRestorationWithoutCollapsingDuplicateNames() {
        val first = PatchMetadata(spec = PatchSpec(hidden = false, dependencies = emptyList(), options = emptyList(),
            bundle = "test",
            id = "first",
            name = "Hide Ads",
            description = "",
            enabledByDefault = true,
            compatibility = Compatibility.Packages(listOf(CompatiblePackage("com.example", emptyList()))),
        ))
        val second = first.copy(spec = first.spec.copy(id = "second"))
        val route = Route.Run(
            target = PatchTarget("Example", "com.example", "1", "app.apk"),
            selection = PatchSelection(disable = emptyList(), options = emptyMap(), enable = listOf(first.reference, second.reference)),
            queue = listOf(first.reference, second.reference),
            bundlePaths = listOf("test.reseam"),
            patches = listOf(first, second),
        )
        val restored = Json.decodeFromString<Route.Run>(Json.encodeToString(route))
        val state = RunState(
            patches = restored.patches.associateBy { it.reference },
            statuses = mapOf(first.reference to PatchStatus.Applied, second.reference to PatchStatus.Failed("failed")),
            results = listOf(
                PatchResult(hidden = false, requiredBy = emptyList(), logs = emptyList(), patch = first.reference, status = PatchStatus.Applied),
                PatchResult(hidden = false, requiredBy = emptyList(), logs = emptyList(), patch = second.reference, status = PatchStatus.Failed("failed")),
            ),
        )
        assertEquals("test/first", first.reference)
        assertEquals("Hide Ads", state.patchName(first.reference))
        assertEquals("Hide Ads", state.patchName(second.reference))
        assertEquals(1, state.applied)
        assertEquals(listOf(second.reference), state.failed)
        val saved = AppliedPatch(second.reference.substringAfter('/'), second.reference.substringBefore('/'), state.patchName(second.reference))
        assertEquals(AppliedPatch("second", "test", "Hide Ads"), saved)
        assertEquals(saved, Json.decodeFromString<AppliedPatch>(Json.encodeToString(saved)))
        val legacy = Json.decodeFromString<AppliedPatch>("""{"id":"Hide Ads","bundle":"test"}""")
        assertEquals("Hide Ads", legacy.name)
    }
}
