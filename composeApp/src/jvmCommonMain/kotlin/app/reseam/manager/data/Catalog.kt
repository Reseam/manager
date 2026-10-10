package app.reseam.manager.data

import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.universal
import app.reseam.sdk.PatchMetadata

data class CatalogApp(val packageName: String, val patchCount: Int, val versions: List<VersionOption>)

data class Catalog(val sources: Int, val apps: List<CatalogApp>, val universal: Int) {
    fun find(packageName: String): CatalogApp? = apps.firstOrNull { it.packageName == packageName }

    fun versions(packageName: String): List<VersionOption> = find(packageName)?.versions ?: versionOptions(emptyList(), universal > 0)

    companion object {
        fun of(patches: Map<String, List<PatchMetadata>>): Catalog {
            val visible = patches.values.flatten().filter { !it.spec.hidden }
            val universal = visible.count { it.universal }
            val apps = visible.flatMap { it.declared }.groupBy { it.`package` }
                .map { (packageName, entries) -> CatalogApp(packageName, entries.size, versionOptions(entries, universal > 0)) }
                .sortedByDescending { it.patchCount }
            return Catalog(patches.size, apps, universal)
        }
    }
}
