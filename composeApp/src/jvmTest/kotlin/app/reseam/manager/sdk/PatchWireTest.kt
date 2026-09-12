package app.reseam.manager.sdk

import app.reseam.manager.data.AppliedPatch
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.run.RunState
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
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
                PatchResult(name = "chosen", status = PatchStatus.Applied),
                PatchResult(name = "dependency", requiredBy = listOf("chosen"), status = PatchStatus.Applied),
                PatchResult(name = "internal", hidden = true, requiredBy = listOf("chosen"), status = PatchStatus.Applied),
                PatchResult(name = "skipped", status = PatchStatus.Skipped("not selected")),
            ),
        )
        assertEquals(1, state.applied)
    }

    @Test
    fun runMetadataSurvivesRestorationWithoutCollapsingDuplicateNames() {
        val first = PatchMetadata(
            bundle = "test",
            id = "app.example.first",
            name = "Hide Ads",
            description = "",
            enabledByDefault = true,
            compatibility = Compatibility.Packages(listOf(CompatiblePackage("com.example"))),
        )
        val second = first.copy(id = "app.example.second")
        val route = Route.Run(
            target = PatchTarget("Example", "com.example", "1", "app.apk"),
            selection = PatchSelection(enable = setOf(first.id, second.id)),
            queue = listOf(first.id, second.id),
            bundlePaths = listOf("test.reseam"),
            patches = listOf(first, second),
        )
        val restored = Json.decodeFromString<Route.Run>(Json.encodeToString(route))
        val state = RunState(
            patches = restored.patches.associateBy { it.id },
            statuses = mapOf(first.id to PatchStatus.Applied, second.id to PatchStatus.Failed("failed")),
            results = listOf(
                PatchResult(name = first.id, status = PatchStatus.Applied),
                PatchResult(name = second.id, status = PatchStatus.Failed("failed")),
            ),
        )
        assertEquals("Hide Ads", state.patchName(first.id))
        assertEquals("Hide Ads", state.patchName(second.id))
        assertEquals(1, state.applied)
        assertEquals(listOf(second.id), state.failed)
        val saved = AppliedPatch(second.id, second.bundle, second.name)
        assertEquals(saved, Json.decodeFromString<AppliedPatch>(Json.encodeToString(saved)))
        val legacy = Json.decodeFromString<AppliedPatch>("""{"id":"Hide Ads","bundle":"test"}""")
        assertEquals("Hide Ads", legacy.name)
    }

}
