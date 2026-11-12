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
import app.reseam.manager.ui.ShownUpdate
import app.reseam.manager.ui.SingleTask
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.shownUpdates
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

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val graph: AppGraph) : ViewModel() {
    private val queryState = MutableStateFlow("")
    val query: StateFlow<String> = queryState.asStateFlow()

    private val everything = MutableStateFlow<List<InstalledApp>?>(null)
    private val showingOthers = MutableStateFlow(false)

    private val picker = SingleTask<Unit>(viewModelScope, graph.notices)
    val picking: StateFlow<Boolean> = picker.busy

    private val refresher = SingleTask<Unit>(viewModelScope, graph.notices)
    val refreshing: StateFlow<Boolean> = refresher.busy

    val canShowAll: Boolean = graph.installedApps != null

    private val library = combine(graph.patchedApps.apps, graph.bundles.catalog, graph.savedApks.apks, graph.appListings.listings, graph.androidRequirements.known, ::HomeLibrarySources)
        .mapLatest { homeLibrary(it, graph.installedApps, graph.androidRequirements) }

    val state: StateFlow<HomeState> = combine(library, everything, showingOthers, queryState) { library, everything, showing, query ->
        library ?: return@combine HomeState()
        val others = everything?.takeIf { showing }?.filter { it.packageName !in library.known }?.sortedBy { it.name.lowercase() }?.map { HomeApp(AppLook(it.name, it.packageName), it.versionName) }
        HomeState(
            loaded = true,
            noPatches = library.noPatches,
            patched = library.patched.matching(query) { it.look },
            ready = library.ready.matching(query) { it.look },
            notInstalled = library.notInstalled.matching(query) { it.look },
            universal = library.universal,
            others = others?.matching(query) { it.look },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    val update: StateFlow<ManagerUpdate?> = graph.managerUpdates.update

    val appUpdates: StateFlow<List<ShownUpdate>> = combine(graph.shownUpdates(), queryState) { updates, query -> updates.matching(query) { it.look } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Why patches are missing, unless a sync is already retrying. */
    val syncFailure: StateFlow<Throwable?> = combine(graph.bundleSyncer.failure, graph.bundles.syncing) { failure, syncing -> failure.takeUnless { syncing } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val bundleUpdate: StateFlow<BundleUpdate?> = graph.bundles.updating
    val updateInstallable: Boolean = graph.managerUpdates.installable

    val bundleOffers: StateFlow<List<BundleOffer>> = graph.bundles.offers

    val sourcesOff: StateFlow<Boolean> = graph.bundles.bundles.map { bundles -> bundles.isNotEmpty() && bundles.none { it.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val whatsNew: StateFlow<List<Bundle>> = graph.bundles.bundles.map { bundles -> bundles.filter { it.enabled && it.whatsNew != null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val announcement: StateFlow<Announcement?> = combine(graph.announcements.feed, graph.settings.settings) { feed, settings ->
        (feed as? AnnouncementFeed.Loaded)?.announcements?.firstOrNull()?.takeIf { it.id > settings.seenAnnouncement }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(query: String) {
        queryState.value = query
    }

    fun showAll() {
        showingOthers.value = true
        if (everything.value != null) return
        viewModelScope.launch {
            everything.value = graph.notices.attempt { graph.installedApps?.all().orEmpty() }.orEmpty()
        }
    }

    fun hideOthers() {
        showingOthers.value = false
    }

    fun open(result: Result<PlatformFile?>, onOpened: (Route.App) -> Unit) = picker.launch(Unit) {
        result.getOrThrow()?.let { graph.apkSources.pick(it) }?.let { onOpened(Route.App(it.packageName, it.source)) }
    }

    fun refresh() = refresher.launch(Unit) { graph.updateChecks.refresh() }

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

    fun updateApp(update: ShownUpdate) = graph.appUpdater.start(listOf(update.update))

    fun updateAllApps() = graph.appUpdater.start(appUpdates.value.filter { it.joinsUpdateAll }.map { it.update })

    fun installUpdate() = graph.managerUpdates.install()

    fun cancelUpdate() = graph.managerUpdates.cancel()
}
