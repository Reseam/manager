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
        dependencies = listOf("test/pinned"),
        compatibility = Compatibility.Packages(listOf(CompatiblePackage("com.example"))),
    )
    private val response = InspectResponse(patches = listOf(pinned, open))

    @Test
    fun untestedPatchesOnlyFollowTogglesWhileAllowed() {
        val strict = PatchEditor.from(response, "com.example", allowIncompatible = false)
        assertFalse(strict.rows.first { it.reference == "test/pinned" }.enabled)
        assertFalse(strict.toggle("test/pinned", true).rows.first { it.reference == "test/pinned" }.enabled)
        assertFalse(strict.selection().ignoreVersions)

        val allowed = strict.allow(true).toggle("test/pinned", true)
        assertTrue(allowed.rows.first { it.reference == "test/pinned" }.enabled)
        assertEquals(1, allowed.enabledIncompatibleCount)
        assertTrue(allowed.selection().ignoreVersions)
        assertEquals(setOf("test/pinned", "test/open"), allowed.selection().enable)

        val withdrawn = allowed.allow(false)
        assertFalse(withdrawn.rows.first { it.reference == "test/pinned" }.enabled)
        assertFalse(withdrawn.selection().ignoreVersions)
    }

    @Test
    fun enablingADependentPullsInAnUntestedDependencyOnlyWhenAllowed() {
        val strict = PatchEditor.from(response, "com.example", allowIncompatible = false).toggle("test/open", false).toggle("test/open", true)
        assertFalse(strict.rows.first { it.reference == "test/pinned" }.enabled)

        val allowed = strict.allow(true).toggle("test/open", false).toggle("test/open", true)
        assertTrue(allowed.rows.first { it.reference == "test/pinned" }.enabled)
    }

    @Test
    fun selectAllLeavesUntestedPatchesAlone() {
        val editor = PatchEditor.from(response, "com.example", allowIncompatible = true).toggle("test/open", false).enableAll()
        assertTrue(editor.rows.first { it.reference == "test/open" }.enabled)
        assertFalse(editor.rows.first { it.reference == "test/pinned" }.enabled)
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
        val middle = base.copy(id = "middle", name = "Middle", dependencies = listOf("test/base"))
        val top = base.copy(id = "top", name = "Top", dependencies = listOf("test/middle"))
        val editor = PatchEditor.from(InspectResponse(patches = listOf(base, middle, top)), "com.example", false)
            .toggle("test/top", true)

        assertTrue(editor.rows.first { it.reference == "test/base" }.enabled)
        assertEquals(listOf("Middle", "Top"), editor.requiredBy("test/base").map { it.meta.name }.sorted())
        assertTrue(editor.requiredBy("test/top").isEmpty())

        // Switching the base off has to take the whole chain with it, not just its direct dependent.
        val off = editor.toggle("test/base", false)
        assertFalse(off.rows.first { it.reference == "test/middle" }.enabled)
        assertFalse(off.rows.first { it.reference == "test/top" }.enabled)
    }

    @Test
    fun sameNamedPatchesKeepSeparateSelectionsAndOptions() {
        val first = open.copy(id = "first", name = "Hide Ads", dependencies = emptyList())
        val second = first.copy(id = "second")
        val editor = PatchEditor.from(InspectResponse(patches = listOf(first, second)), "com.example", false)
            .setOption(first.reference, "value", OptionValue.Text("first"))
            .setOption(second.reference, "value", OptionValue.Text("second"))
        assertEquals(listOf(first.reference, second.reference), editor.queue())
        assertEquals(OptionValue.Text("first"), editor.selection().options[first.reference]?.get("value"))
        assertEquals(OptionValue.Text("second"), editor.selection().options[second.reference]?.get("value"))
        val selection = editor.toggle(first.reference, false).selection()
        assertEquals(setOf(second.reference), selection.enable)
        assertEquals(setOf(first.reference), selection.disable)
        assertEquals(setOf(second.reference), selection.options.keys)
    }

}
