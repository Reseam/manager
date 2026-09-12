package app.reseam.manager.sdk

import app.reseam.manager.data.AppliedPatch
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.patches.PatchEditor
import app.reseam.manager.ui.run.RunState
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PatchWireTest {
    @Test
    fun automaticRequestUsesTheSdkDiscriminator() {
        val request = PatchRequest(apkPath = "app.xapk", bundlePaths = emptyList(), output = PatchOutput.Auto("com.example.reseamed"))
        val output = WireJson.parseToJsonElement(WireJson.encodeToString(request)).jsonObject.getValue("output").jsonObject
        assertEquals("auto", output.getValue("kind").jsonPrimitive.content)
        assertEquals("com.example.reseamed", output.getValue("path").jsonPrimitive.content)
    }

    @Test
    fun outcomeCarriesTheResolvedArtifactForBothOutputShapes() {
        for ((kind, path) in listOf("single_file" to "com.example.reseamed.apk", "split_dir" to "com.example.reseamed")) {
            val outcome = WireJson.decodeFromString<PatchOutcome>("""
                {"results":[],"metrics":{"total_duration_ms":1},"output":{"kind":"$kind","path":"$path"}}
            """.trimIndent())
            assertEquals(path, outcome.output.path)
            if (kind == "single_file") assertIs<PatchOutput.SingleFile>(outcome.output)
            else assertIs<PatchOutput.SplitDir>(outcome.output)
        }
    }

    @Test
    fun nativeSdkAcceptsAutomaticOutputAndRejectsAnInvalidContainerBeforeLoadingBundles() = runTest {
        val directory = createTempDirectory("reseam-wire-test").toFile()
        try {
            val input = directory.resolve("empty.apkm")
            ZipOutputStream(input.outputStream()).use { }
            val error = assertFailsWith<Exception> {
                ReseamSdk.patch(PatchRequest(
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
                PatchResult(patch = "test/chosen", status = PatchStatus.Applied),
                PatchResult(patch = "test/dependency", requiredBy = listOf("test/chosen"), status = PatchStatus.Applied),
                PatchResult(patch = "test/internal", hidden = true, requiredBy = listOf("test/chosen"), status = PatchStatus.Applied),
                PatchResult(patch = "test/skipped", status = PatchStatus.Skipped("not selected")),
            ),
        )
        assertEquals(1, state.applied)
    }

    @Test
    fun resultsAndEventsNameThePatchByReference() {
        val result = WireJson.decodeFromString<PatchResult>("""
            {"patch":"test/hide-ads","hidden":false,"required_by":["test/premium"],"status":{"kind":"applied"},"logs":[{"level":"INFO","patch":"test/hide-ads","message":"done"}]}
        """.trimIndent())
        assertEquals("test/hide-ads", result.patch)
        assertEquals(listOf("test/premium"), result.requiredBy)
        assertEquals("test/hide-ads", result.logs.single().patch)
        val finished = WireJson.decodeFromString<RunEvent>("""{"type":"patch_finished","patch":"test/hide-ads","status":{"kind":"applied"}}""")
        assertEquals("test/hide-ads", assertIs<RunEvent.PatchFinished>(finished).patch)
    }

    @Test
    fun theSelectionSentToTheEngineUsesReferences() {
        val first = PatchMetadata(bundle = "one", id = "hide-ads", name = "Hide Ads", description = "", enabledByDefault = true, compatibility = Compatibility.Universal)
        val second = first.copy(bundle = "two", enabledByDefault = false)
        val editor = PatchEditor.from(InspectResponse(patches = listOf(first, second)), packageName = null, allowIncompatible = false)
        assertEquals(listOf("one/hide-ads", "two/hide-ads"), editor.rows.map { it.reference })
        val selection = WireJson.parseToJsonElement(WireJson.encodeToString(editor.selection())).jsonObject
        assertEquals(listOf("one/hide-ads"), selection.getValue("enable").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("two/hide-ads"), selection.getValue("disable").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(listOf("one/hide-ads"), editor.queue())
    }

    @Test
    fun runMetadataSurvivesRestorationWithoutCollapsingDuplicateNames() {
        val first = PatchMetadata(
            bundle = "test",
            id = "first",
            name = "Hide Ads",
            description = "",
            enabledByDefault = true,
            compatibility = Compatibility.Packages(listOf(CompatiblePackage("com.example"))),
        )
        val second = first.copy(id = "second")
        val route = Route.Run(
            target = PatchTarget("Example", "com.example", "1", "app.apk"),
            selection = PatchSelection(enable = setOf(first.reference, second.reference)),
            queue = listOf(first.reference, second.reference),
            bundlePaths = listOf("test.reseam"),
            patches = listOf(first, second),
        )
        val restored = Json.decodeFromString<Route.Run>(Json.encodeToString(route))
        val state = RunState(
            patches = restored.patches.associateBy { it.reference },
            statuses = mapOf(first.reference to PatchStatus.Applied, second.reference to PatchStatus.Failed("failed")),
            results = listOf(
                PatchResult(patch = first.reference, status = PatchStatus.Applied),
                PatchResult(patch = second.reference, status = PatchStatus.Failed("failed")),
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
