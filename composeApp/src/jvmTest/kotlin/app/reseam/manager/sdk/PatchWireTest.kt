package app.reseam.manager.sdk

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

}
