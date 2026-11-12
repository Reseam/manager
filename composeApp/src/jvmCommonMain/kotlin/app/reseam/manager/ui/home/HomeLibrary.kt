package app.reseam.manager.ui.home

import app.reseam.manager.data.AndroidRequirementLibrary
import app.reseam.manager.data.AndroidRequirements
import app.reseam.manager.data.AppListing
import app.reseam.manager.data.Catalog
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.data.SavedApk
import app.reseam.manager.data.preferredFor
import app.reseam.manager.platform.InstalledApps
import app.reseam.manager.ui.components.AppLook
import app.reseam.sdk.InstallMethod

data class HomeLibrarySources(
    val apps: List<PatchedApp>,
    val catalog: Catalog?,
    val saved: List<SavedApk>,
    val listings: Map<String, AppListing>,
    val known: AndroidRequirementLibrary,
)

data class HomeLibrary(
    val noPatches: Boolean,
    val universal: Int,
    val known: Set<String>,
    val patched: List<HomeApp>,
    val ready: List<HomeApp>,
    val notInstalled: List<HomeApp>,
)

/** Home's sections from the library, the catalog, saved APKs and app listings; null until the catalog loads. */
suspend fun homeLibrary(sources: HomeLibrarySources, installedApps: InstalledApps?, requirements: AndroidRequirements): HomeLibrary? {
    val (apps, catalog, saved, listings, known) = sources
    if (catalog == null) return null
    val patchedNames = apps.map { it.packageName }.toSet()
    val installed = installedApps?.query(catalog.apps.map { it.packageName })?.associateBy { it.packageName }
    val available = catalog.apps.filter { it.packageName !in patchedNames }
    val onDevice = available.mapNotNull { app ->
        installed?.get(app.packageName)?.let { HomeApp(AppLook(it.name, it.packageName), it.versionName, app.patchCount) }
    }
    val (fromSaved, toDownload) = available.filter { installed?.containsKey(it.packageName) != true }.map { app ->
        val recommended = requirements.fit(app.packageName, app.versions, known).first().versionName
        val preferred = saved.preferredFor(app.packageName, recommended)
        val copy = preferred ?: saved.filter { it.packageName == app.packageName }.maxByOrNull { it.savedAtEpochMs }
        val listing = listings[app.packageName]
        val look = AppLook(copy?.name ?: listing?.name ?: app.packageName, app.packageName, copy?.iconPath ?: listing?.iconPath)
        HomeApp(look, preferred?.versionName ?: recommended, app.patchCount, saved = preferred != null)
    }.partition { it.saved }
    return HomeLibrary(
        noPatches = catalog.sources == 0,
        universal = catalog.universal,
        known = patchedNames + catalog.apps.map { it.packageName },
        patched = apps.sortedByDescending { it.patchedAtEpochMs }.distinctBy { it.packageName }
            .map { HomeApp(AppLook(it.name, it.packageName, it.iconPath), it.versionName, applied = it.patches.size, mounted = it.installMethod == InstallMethod.MOUNT) },
        ready = if (installed == null) fromSaved + toDownload else onDevice + fromSaved,
        notInstalled = if (installed == null) emptyList() else toDownload,
    )
}

fun <T> List<T>.matching(query: String, look: (T) -> AppLook): List<T> {
    val needle = query.trim().lowercase()
    return if (needle.isEmpty()) this else filter { look(it).let { app -> needle in app.name.lowercase() || needle in app.packageName } }
}
