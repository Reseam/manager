package app.reseam.manager.ui.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import app.reseam.manager.resources.*
import app.reseam.manager.AppGraph
import app.reseam.manager.ui.announcements.AnnouncementScreen
import app.reseam.manager.ui.announcements.AnnouncementViewModel
import app.reseam.manager.ui.announcements.UpdatesScreen
import app.reseam.manager.ui.announcements.UpdatesViewModel
import app.reseam.manager.ui.app.AppPageScreen
import app.reseam.manager.ui.app.AppPageViewModel
import app.reseam.manager.ui.app.PatchSessions
import app.reseam.manager.ui.app.PatchesScreen
import app.reseam.manager.ui.home.HomePlaceholder
import app.reseam.manager.ui.home.HomeScreen
import app.reseam.manager.ui.home.HomeViewModel
import app.reseam.manager.ui.run.RunScreen
import app.reseam.manager.ui.run.RunViewModel
import app.reseam.manager.ui.settings.AddSourceSheet
import app.reseam.manager.ui.settings.AddSourceViewModel
import app.reseam.manager.ui.settings.LicensesScreen
import app.reseam.manager.ui.settings.LicensesViewModel
import app.reseam.manager.ui.settings.PatchScreen
import app.reseam.manager.ui.settings.PatchViewModel
import app.reseam.manager.ui.settings.ReleaseScreen
import app.reseam.manager.ui.settings.ReleasesScreen
import app.reseam.manager.ui.settings.SavedApksScreen
import app.reseam.manager.ui.settings.SavedApksViewModel
import app.reseam.manager.ui.settings.SettingsScreen
import app.reseam.manager.ui.settings.SettingsViewModel
import app.reseam.manager.ui.settings.SigningKeyScreen
import app.reseam.manager.ui.settings.SigningKeyViewModel
import app.reseam.manager.ui.settings.SourceScreen
import app.reseam.manager.ui.settings.SourceViewModel
import app.reseam.manager.ui.settings.SourcesScreen
import app.reseam.manager.ui.settings.SourcesViewModel
import app.reseam.manager.ui.settings.WhatsNewScreen
import app.reseam.manager.ui.settings.WhatsNewViewModel
import app.reseam.manager.ui.settings.bundleReleases
import app.reseam.manager.ui.settings.release
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.jetbrains.compose.resources.stringResource

private val NavState = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Home::class)
            subclass(Route.App::class)
            subclass(Route.Patches::class)
            subclass(Route.Run::class)
            subclass(Route.Settings::class)
            subclass(Route.Sources::class)
            subclass(Route.Source::class)
            subclass(Route.Patch::class)
            subclass(Route.SigningKey::class)
            subclass(Route.SavedApks::class)
            subclass(Route.Licenses::class)
            subclass(Route.Updates::class)
            subclass(Route.Announcement::class)
            subclass(Route.WhatsNew::class)
            subclass(Route.Releases::class)
            subclass(Route.Release::class)
        }
    }
}

private val SlideDistance = 30.dp

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalTime::class)
@Composable
fun AppNavigation(graph: AppGraph) {
    val backStack = rememberNavBackStack(NavState, Route.Home)
    val sessions = remember(graph) { PatchSessions(graph) }
    LaunchedEffect(sessions) {
        snapshotFlow { backStack.mapNotNull { (it as Route).packageName }.toSet() }.collect(sessions::retain)
    }

    fun open(from: Route, route: Route) {
        val index = backStack.lastIndexOf(from)
        while (backStack.size > index + 1) backStack.removeAt(backStack.lastIndex)
        backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun home() {
        backStack.retainAll { it == Route.Home }
    }

    fun replace(route: Route) {
        backStack[backStack.lastIndex] = route
    }

    /** Shows [route] over whatever is open, going back to it when it's already in the stack. */
    fun show(route: Route) {
        val index = backStack.indexOf(route)
        if (index < 0) backStack.add(route) else while (backStack.size > index + 1) backStack.removeAt(backStack.lastIndex)
    }

    val slide = with(LocalDensity.current) { SlideDistance.roundToPx() }
    val forward = remember(slide) { sharedAxis(slide) }
    val backward = remember(slide) { sharedAxis(-slide) }
    val twoPane = remember {
        TwoPaneStrategy { list ->
            when (list) {
                Route.Settings -> SourcesScreen(viewModel { SourcesViewModel(graph) }, onBack = {}, onOpen = { open(Route.Settings, it) })
                else -> HomePlaceholder()
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= TwoPaneWidth
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavDisplay(
                    backStack = backStack,
                    onBack = ::pop,
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                    sceneStrategies = if (wide) listOf(twoPane) else emptyList(),
                    transitionSpec = { forward },
                    popTransitionSpec = { backward },
                    predictivePopTransitionSpec = { backward },
                    entryProvider = entryProvider {
                        entry<Route.Home>(metadata = { paneRoles(it, list = true) }) {
                            HomeScreen(
                                viewModel = viewModel { HomeViewModel(graph) },
                                onOpen = { open(Route.Home, it) },
                                onSettings = { open(Route.Home, Route.Settings) },
                                onUpdates = { open(Route.Home, Route.Updates) },
                            )
                        }
                        entry<Route.App>(metadata = { paneRoles(it, list = true, detail = true) + ContainerTransition }) { route ->
                            val session = sessions.get(route.packageName, route.picked)
                            AppPageScreen(
                                viewModel = viewModel { AppPageViewModel(graph, session) },
                                onBack = ::pop,
                                onSelectPatches = { open(route, Route.Patches(route.packageName)) },
                                onRun = { open(route, it) },
                            )
                        }
                        entry<Route.Patches>(metadata = { paneRoles(it, detail = true) }) { route ->
                            PatchesScreen(sessions.get(route.packageName), onDone = ::pop)
                        }
                        entry<Route.Run> { route ->
                            RunScreen(
                                viewModel = viewModel { RunViewModel(graph, route, createSavedStateHandle()) },
                                onClose = ::pop,
                                onFinished = ::home,
                                onInstallInstead = { replace(route.copy(mount = false, startedAtEpochMs = Clock.System.now().toEpochMilliseconds())) },
                            )
                        }
                        entry<Route.Settings>(metadata = { paneRoles(it, list = true) }) {
                            SettingsScreen(viewModel { SettingsViewModel(graph) }, onBack = ::pop, onOpen = { open(Route.Settings, it) })
                        }
                        entry<Route.Sources>(metadata = { paneRoles(it, list = true, detail = true) }) {
                            SourcesScreen(viewModel { SourcesViewModel(graph) }, onBack = ::pop, onOpen = { open(Route.Sources, it) })
                        }
                        entry<Route.Source>(metadata = { paneRoles(it, list = true, detail = true) }) { route ->
                            SourceScreen(viewModel { SourceViewModel(graph, route.id) }, onBack = ::pop, onOpen = { open(route, it) })
                        }
                        entry<Route.Patch>(metadata = { paneRoles(it, detail = true) }) { route ->
                            PatchScreen(viewModel { PatchViewModel(graph, route.bundleId, route.patchId) }, onBack = ::pop)
                        }
                        entry<Route.SigningKey>(metadata = { paneRoles(it, detail = true) }) {
                            SigningKeyScreen(viewModel { SigningKeyViewModel(graph) }, onBack = ::pop)
                        }
                        entry<Route.SavedApks>(metadata = { paneRoles(it, detail = true) }) {
                            SavedApksScreen(viewModel { SavedApksViewModel(graph) }, onBack = ::pop)
                        }
                        entry<Route.Licenses>(metadata = { paneRoles(it, detail = true) }) {
                            LicensesScreen(viewModel { LicensesViewModel() }, onBack = ::pop)
                        }
                        entry<Route.Updates>(metadata = { paneRoles(it, list = true, detail = true) }) {
                            UpdatesScreen(viewModel { UpdatesViewModel(graph) }, onBack = ::pop, onOpen = { open(Route.Updates, it) })
                        }
                        entry<Route.Announcement>(metadata = { paneRoles(it, detail = true) }) { route ->
                            AnnouncementScreen(viewModel { AnnouncementViewModel(graph, route.id) }, onBack = ::pop)
                        }
                        entry<Route.WhatsNew>(metadata = { paneRoles(it, detail = true) }) { route ->
                            WhatsNewScreen(viewModel { WhatsNewViewModel(graph, route.bundleId) }, onBack = ::pop)
                        }
                        entry<Route.Releases>(metadata = { paneRoles(it, detail = true) }) { route ->
                            ReleasesScreen(viewModel { bundleReleases(graph, route.bundleId) }, onBack = ::pop)
                        }
                        entry<Route.Release>(metadata = { paneRoles(it, detail = true) }) { route ->
                            val product = stringResource(if (route.bundleId == null) Res.string.app_name else Res.string.updates_patches)
                            ReleaseScreen(viewModel { release(graph, route.bundleId, route.version) }, title = product, onBack = ::pop)
                        }
                    },
                )
            }
        }
    }
    AddSourceSheet(viewModel { AddSourceViewModel(graph) }, onOpen = ::show)
}
