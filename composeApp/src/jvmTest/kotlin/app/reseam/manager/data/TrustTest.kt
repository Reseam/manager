package app.reseam.manager.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private const val SelfHostedApi = "https://patches.example.invalid/v1"
private val otherKey = "ab".repeat(32)

class OfficialSignerPromptTest {
    @Test
    fun pinnedKeyAtTheDefaultApiIsTrusted() {
        assertNull(officialSignerPrompt(DefaultApiBaseUrl, OfficialSignerKey, null))
    }

    @Test
    fun anyOtherKeyAtTheDefaultApiIsRejected() {
        assertFailsWith<OfficialSignerMismatch> { officialSignerPrompt(DefaultApiBaseUrl, otherKey, null) }
        assertFailsWith<OfficialSignerMismatch> { officialSignerPrompt(DefaultApiBaseUrl, otherKey, bundle(otherKey)) }
    }

    @Test
    fun firstKeyFromAnotherApiNeedsConfirmation() {
        assertEquals(TrustPrompt.NewApiSigner, officialSignerPrompt(SelfHostedApi, otherKey, null))
        assertEquals(TrustPrompt.NewApiSigner, officialSignerPrompt(SelfHostedApi, OfficialSignerKey, null))
    }

    @Test
    fun theInstalledSignerFromAnotherApiIsTrusted() {
        assertNull(officialSignerPrompt(SelfHostedApi, otherKey, bundle(otherKey)))
    }

    @Test
    fun aChangedSignerFromAnotherApiNeedsConfirmationWithThePreviousKey() {
        assertEquals(TrustPrompt.ChangedApiSigner(otherKey), officialSignerPrompt(SelfHostedApi, OfficialSignerKey, bundle(otherKey)))
    }

    private fun bundle(key: String) = Bundle(
        id = key,
        name = "bundle",
        author = "",
        description = "",
        version = "1",
        official = true,
        origin = SelfHostedApi,
        path = "/nowhere",
        patches = emptyList(),
    )
}
