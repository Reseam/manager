package app.reseam.manager.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LicensedArtifact(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val name: String? = null,
    val spdxLicenses: List<SpdxLicense> = emptyList(),
    val unknownLicenses: List<UnknownLicense> = emptyList(),
)

@Serializable
data class SpdxLicense(val identifier: String, val name: String, val url: String)

@Serializable
data class UnknownLicense(val name: String? = null, val url: String? = null)

data class Acknowledgement(val name: String, val version: String?, val licenses: List<String>, val url: String?)

val BundledAssets = listOf(
    Acknowledgement("Inter", "4.1", listOf("SIL Open Font License 1.1"), "https://github.com/rsms/inter/blob/master/LICENSE.txt"),
    Acknowledgement("JetBrains Mono", "2.304", listOf("SIL Open Font License 1.1"), "https://github.com/JetBrains/JetBrainsMono/blob/master/OFL.txt"),
    Acknowledgement("Lucide", null, listOf("ISC License"), "https://lucide.dev/license"),
)

private val ReportJson = Json { ignoreUnknownKeys = true }

fun acknowledgements(report: String): List<Acknowledgement> =
    ReportJson.decodeFromString<List<LicensedArtifact>>(report)
        .map { artifact ->
            Acknowledgement(
                name = artifact.name ?: "${artifact.groupId}:${artifact.artifactId}",
                version = artifact.version,
                licenses = artifact.spdxLicenses.map { it.name } + artifact.unknownLicenses.mapNotNull { it.name },
                url = artifact.spdxLicenses.firstOrNull()?.url ?: artifact.unknownLicenses.firstNotNullOfOrNull { it.url },
            )
        }
        .distinctBy { it.name to it.licenses }
        .sortedBy { it.name.lowercase() }
