package app.reseam.manager.ui.patches

import app.reseam.manager.sdk.Compatibility
import app.reseam.manager.sdk.CompatiblePackage
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
        compatibility = Compatibility.Packages(listOf(CompatiblePackage("com.example", listOf("1.0")))),
        incompatibility = "expected one of [1.0], got 2.0",
    )
    private val open = PatchMetadata(
        bundle = "test",
        id = "open",
        description = "",
        enabledByDefault = true,
        dependencies = listOf("pinned"),
        compatibility = Compatibility.Packages(listOf(CompatiblePackage("com.example"))),
    )
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
    fun aDependencyNamesWhatKeepsItOnAndGoesOffWithIt() {
        val base = PatchMetadata(
            bundle = "test",
            id = "base",
            name = "Base",
            description = "",
            enabledByDefault = false,
            compatibility = Compatibility.Packages(listOf(CompatiblePackage("com.example"))),
        )
        val middle = base.copy(id = "middle", name = "Middle", dependencies = listOf("base"))
        val top = base.copy(id = "top", name = "Top", dependencies = listOf("middle"))
        val editor = PatchEditor.from(InspectResponse(patches = listOf(base, middle, top)), "com.example", false)
            .toggle("top", true)

        assertTrue(editor.rows.first { it.id == "base" }.enabled)
        assertEquals(listOf("Middle", "Top"), editor.requiredBy("base").map { it.meta.name }.sorted())
        assertTrue(editor.requiredBy("top").isEmpty())

        // Switching the base off has to take the whole chain with it, not just its direct dependent.
        val off = editor.toggle("base", false)
        assertFalse(off.rows.first { it.id == "middle" }.enabled)
        assertFalse(off.rows.first { it.id == "top" }.enabled)
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
