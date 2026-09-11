package app.reseam.manager.ui.patches

import app.reseam.manager.sdk.Compatibility
import app.reseam.manager.sdk.InspectResponse
import app.reseam.manager.sdk.OptionValue
import app.reseam.manager.sdk.PatchMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PatchEditorTest {
    private val pinned = PatchMetadata(
        bundle = "test",
        id = "pinned",
        description = "",
        enabledByDefault = true,
        compatibility = listOf(Compatibility("com.example", listOf("1.0"))),
        incompatibility = "expected one of [1.0], got 2.0",
    )
    private val open = PatchMetadata(bundle = "test", id = "open", description = "", enabledByDefault = true, dependencies = listOf("pinned"))
    private val response = InspectResponse(patches = listOf(pinned, open))

    @Test
    fun untestedPatchesOnlyFollowTogglesWhileAllowed() {
        val strict = PatchEditor.from(response, "com.example", allowIncompatible = false)
        assertFalse(strict.rows.first { it.id == "pinned" }.enabled)
        assertFalse(strict.toggle("pinned", true).rows.first { it.id == "pinned" }.enabled)
        assertFalse(strict.selection().ignoreVersions)

        val allowed = strict.allow(true).toggle("pinned", true)
        assertTrue(allowed.rows.first { it.id == "pinned" }.enabled)
        assertEquals(1, allowed.enabledIncompatibleCount)
        assertTrue(allowed.selection().ignoreVersions)
        assertEquals(setOf("pinned", "open"), allowed.selection().enable)

        val withdrawn = allowed.allow(false)
        assertFalse(withdrawn.rows.first { it.id == "pinned" }.enabled)
        assertFalse(withdrawn.selection().ignoreVersions)
    }

    @Test
    fun enablingADependentPullsInAnUntestedDependencyOnlyWhenAllowed() {
        val strict = PatchEditor.from(response, "com.example", allowIncompatible = false).toggle("open", false).toggle("open", true)
        assertFalse(strict.rows.first { it.id == "pinned" }.enabled)

        val allowed = strict.allow(true).toggle("open", false).toggle("open", true)
        assertTrue(allowed.rows.first { it.id == "pinned" }.enabled)
    }

    @Test
    fun selectAllLeavesUntestedPatchesAlone() {
        val editor = PatchEditor.from(response, "com.example", allowIncompatible = true).toggle("open", false).enableAll()
        assertTrue(editor.rows.first { it.id == "open" }.enabled)
        assertFalse(editor.rows.first { it.id == "pinned" }.enabled)
    }
    @Test
    fun sameNamedPatchesKeepSeparateSelectionsAndOptions() {
        val first = open.copy(id = "app.example.first", name = "Hide Ads", dependencies = emptyList())
        val second = first.copy(id = "app.example.second")
        val editor = PatchEditor.from(InspectResponse(patches = listOf(first, second)), "com.example", false)
            .setOption(first.id, "value", OptionValue.Text("first"))
            .setOption(second.id, "value", OptionValue.Text("second"))
        assertEquals(listOf(first.id, second.id), editor.queue())
        assertEquals(OptionValue.Text("first"), editor.selection().options[first.id]?.get("value"))
        assertEquals(OptionValue.Text("second"), editor.selection().options[second.id]?.get("value"))
        val selection = editor.toggle(first.id, false).selection()
        assertEquals(setOf(second.id), selection.enable)
        assertEquals(setOf(first.id), selection.disable)
        assertEquals(setOf(second.id), selection.options.keys)
    }

}
