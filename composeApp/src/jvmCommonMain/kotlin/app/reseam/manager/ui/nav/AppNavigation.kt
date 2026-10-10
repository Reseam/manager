package app.reseam.manager.ui.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import app.reseam.manager.AppGraph
import app.reseam.manager.ui.announcements.AnnouncementScreen
import app.reseam.manager.ui.announcements.AnnouncementViewModel
import app.reseam.manager.ui.announcements.AnnouncementsScreen
import app.reseam.manager.ui.announcements.AnnouncementsViewModel
import app.reseam.manager.ui.app.AppPageScreen
import app.reseam.manager.ui.app.AppPageViewModel
import app.reseam.manager.ui.app.PatchSessions
import app.reseam.manager.ui.app.PatchesScreen
import app.reseam.manager.ui.home.HomePlaceholder
import app.reseam.manager.ui.home.HomeScreen
import app.reseam.manager.ui.home.HomeViewModel
import app.reseam.manager.ui.run.RunScreen
import app.reseam.manager.ui.run.RunViewModel
import app.reseam.manager.ui.settings.PatchScreen
import app.reseam.manager.ui.settings.PatchViewModel
import app.reseam.manager.ui.settings.ReleasesScreen
import app.reseam.manager.ui.settings.ReleasesViewModel
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
import app.reseam.manager.ui.theme.Motion
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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
            subclass(Route.Announcements::class)
            subclass(Route.Announcement::class)
            subclass(Route.WhatsNew::class)
            subclass(Route.Releases::class)
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
                            )
                        }
                        entry<Route.App>(metadata = { paneRoles(it, list = true, detail = true) }) { route ->
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
                        entry<Route.Announcements>(metadata = { paneRoles(it, list = true, detail = true) }) {
                            AnnouncementsScreen(viewModel { AnnouncementsViewModel(graph) }, onBack = ::pop, onOpen = { open(Route.Announcements, it) })
                        }
                        entry<Route.Announcement>(metadata = { paneRoles(it, detail = true) }) { route ->
                            AnnouncementScreen(viewModel { AnnouncementViewModel(graph, route.id) }, onBack = ::pop)
                        }
                        entry<Route.WhatsNew>(metadata = { paneRoles(it, detail = true) }) { route ->
                            WhatsNewScreen(viewModel { WhatsNewViewModel(graph, route.bundleId) }, onBack = ::pop)
                        }
                        entry<Route.Releases>(metadata = { paneRoles(it, detail = true) }) { route ->
                            ReleasesScreen(viewModel { ReleasesViewModel(graph, route.bundleId) }, onBack = ::pop)
                        }
                    },
                )
            }
        }
    }
}

private fun sharedAxis(slide: Int): ContentTransform {
    val enter = tween<Float>(Motion.medium2 - Motion.medium2 * 35 / 100, delayMillis = Motion.medium2 * 35 / 100, easing = Motion.emphasizedDecelerate)
    val exit = tween<Float>(Motion.medium2 * 35 / 100, easing = Motion.emphasizedAccelerate)
    return (slideInHorizontally(tween(Motion.medium2, easing = Motion.emphasized)) { slide } + fadeIn(enter)) togetherWith
        (slideOutHorizontally(tween(Motion.medium2, easing = Motion.emphasized)) { -slide } + fadeOut(exit))
}
