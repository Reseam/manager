package app.reseam.manager.ui.download

import app.reseam.sdk.CompatiblePackage
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionOptionsTest {
    @Test
    fun versionOptionsRankPinnedVersionsByPatchesThenNewestAndOfferLatestWhenAnyVersionWorks() {
        val entries = listOf(
            CompatiblePackage(`package` = "com.example", versions = listOf("1.2.0", "1.10.0")),
            CompatiblePackage(`package` = "com.example", versions = listOf("1.10.0")),
            CompatiblePackage(`package` = "com.example", versions = listOf("1.9.0")),
            CompatiblePackage(`package` = "com.example", versions = emptyList()),
        )
        assertEquals(
            listOf(VersionOption("1.10.0", 3), VersionOption("1.9.0", 2), VersionOption("1.2.0", 2), VersionOption(null, 1)),
            versionOptions(entries, universal = 0),
        )
        assertEquals(listOf(VersionOption("2.0", 1)), versionOptions(listOf(CompatiblePackage(`package` = "com.example", versions = listOf("2.0"))), universal = 0))
        assertEquals(listOf(VersionOption(null, 2)), versionOptions(listOf(CompatiblePackage(`package` = "com.example", versions = emptyList())), universal = 1))
    }
}
