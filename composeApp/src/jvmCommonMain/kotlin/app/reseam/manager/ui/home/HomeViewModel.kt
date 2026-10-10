package app.reseam.manager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reseam.manager.AppGraph
import app.reseam.manager.data.Announcement
import app.reseam.manager.data.AnnouncementFeed
import app.reseam.manager.data.Bundle
import app.reseam.manager.data.BundleOffer
import app.reseam.manager.data.BundleUpdate
import app.reseam.manager.data.ManagerUpdate
import app.reseam.manager.platform.InstalledApp
import app.reseam.manager.ui.SingleTask
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.nav.Route
import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeApp(val look: AppLook, val versionName: String?, val patchCount: Int? = null, val applied: Int? = null, val mounted: Boolean = false)

data class HomeState(
    val loaded: Boolean = false,
    val noPatches: Boolean = false,
    val patched: List<HomeApp> = emptyList(),
    val ready: List<HomeApp> = emptyList(),
    val notInstalled: List<HomeApp> = emptyList(),
    val universal: Int = 0,
    val others: List<HomeApp>? = null,
) {
    val empty: Boolean get() = patched.isEmpty() && ready.isEmpty() && notInstalled.isEmpty() && others.isNullOrEmpty()
}

private data class Library(
    val noPatches: Boolean,
    val universal: Int,
    val known: Set<String>,
    val patched: List<HomeApp>,
    val ready: List<HomeApp>,
    val notInstalled: List<HomeApp>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val graph: AppGraph) : ViewModel() {
    private val queryState = MutableStateFlow("")
    val query: StateFlow<String> = queryState.asStateFlow()

    private val everything = MutableStateFlow<List<InstalledApp>?>(null)

    private val picker = SingleTask<Unit>(viewModelScope, graph.notices)
    val picking: StateFlow<Boolean> = picker.busy

    val canShowAll: Boolean = graph.installedApps != null

    private val library = combine(graph.patchedApps.apps, graph.bundles.catalog, graph.savedApks.apks) { apps, catalog, saved -> Triple(apps, catalog, saved) }
        .mapLatest { (apps, catalog, saved) ->
            if (catalog == null) return@mapLatest null
            val patchedNames = apps.map { it.packageName }.toSet()
            val installed = graph.installedApps?.query(catalog.apps.map { it.packageName })?.associateBy { it.packageName }
            val available = catalog.apps.filter { it.packageName !in patchedNames }
            val onDevice = available.mapNotNull { app ->
                installed?.get(app.packageName)?.let { HomeApp(AppLook(it.name, it.packageName), it.versionName, app.patchCount) }
            }
            val downloadable = available.filter { installed?.containsKey(it.packageName) != true }.map { app ->
                val copy = saved.firstOrNull { it.packageName == app.packageName }
                HomeApp(AppLook(copy?.name ?: app.packageName, app.packageName, copy?.iconPath), app.versions.first().versionName, app.patchCount)
            }
            Library(
                noPatches = catalog.sources == 0,
                universal = catalog.universal,
                known = patchedNames + catalog.apps.map { it.packageName },
                patched = apps.sortedByDescending { it.patchedAtEpochMs }
                    .map { HomeApp(AppLook(it.name, it.packageName, it.iconPath), it.versionName, applied = it.patches.size, mounted = it.installMethod == InstallMethod.MOUNT) },
                ready = if (installed == null) downloadable else onDevice,
                notInstalled = if (installed == null) emptyList() else downloadable,
            )
        }

    val state: StateFlow<HomeState> = combine(library, everything, queryState) { library, everything, query ->
        library ?: return@combine HomeState()
        val others = everything?.filter { it.packageName !in library.known }?.sortedBy { it.name.lowercase() }?.map { HomeApp(AppLook(it.name, it.packageName), it.versionName) }
        HomeState(
            loaded = true,
            noPatches = library.noPatches,
            patched = library.patched.matching(query),
            ready = library.ready.matching(query),
            notInstalled = library.notInstalled.matching(query),
            universal = library.universal,
            others = others?.matching(query),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    val update: StateFlow<ManagerUpdate?> = graph.managerUpdates.update

    /** Why patches are missing, unless a sync is already retrying. */
    val syncFailure: StateFlow<Throwable?> = combine(graph.bundleSyncer.failure, graph.bundles.syncing) { failure, syncing -> failure.takeUnless { syncing } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val bundleUpdate: StateFlow<BundleUpdate?> = graph.bundles.updating
    val updateInstallable: Boolean = graph.managerUpdates.installable

    val bundleOffers: StateFlow<List<BundleOffer>> = graph.bundles.offers

    val whatsNew: StateFlow<List<Bundle>> = graph.bundles.bundles.map { bundles -> bundles.filter { it.whatsNew != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val announcement: StateFlow<Announcement?> = combine(graph.announcements.feed, graph.settings.settings) { feed, settings ->
        (feed as? AnnouncementFeed.Loaded)?.announcements?.firstOrNull()?.takeIf { it.id > settings.seenAnnouncement }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(query: String) {
        queryState.value = query
    }

    fun showAll() {
        if (everything.value != null) return
        viewModelScope.launch {
            everything.value = graph.notices.attempt { graph.installedApps?.all().orEmpty() }.orEmpty()
        }
    }

    fun open(result: Result<PlatformFile?>, onOpened: (Route.App) -> Unit) = picker.launch(Unit) {
        result.getOrThrow()?.let { graph.apkSources.pick(it) }?.let { onOpened(Route.App(it.packageName, it.source)) }
    }

    fun retrySync() {
        graph.bundleSyncer.sync(force = true)
    }

    fun updatePatches(offer: BundleOffer) {
        graph.bundleSyncer.update(offer)
    }

    fun dismissWhatsNew(id: String) {
        viewModelScope.launch { graph.notices.attempt { graph.bundles.dismissWhatsNew(id) } }
    }

    fun dismissAnnouncement(id: Long) {
        viewModelScope.launch { graph.announcements.markSeen(id) }
    }

    fun installUpdate() = graph.managerUpdates.install()

    fun cancelUpdate() = graph.managerUpdates.cancel()
}

private fun List<HomeApp>.matching(query: String): List<HomeApp> {
    val needle = query.trim().lowercase()
    return if (needle.isEmpty()) this else filter { needle in it.look.name.lowercase() || needle in it.look.packageName }
}
