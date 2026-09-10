package app.reseam.manager.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import app.reseam.manager.LocalAppGraph
import app.reseam.manager.platform.Permissions
import app.reseam.manager.ui.appdetail.AppDetailScreen
import app.reseam.manager.ui.appdetail.AppDetailViewModel
import app.reseam.manager.ui.bundles.BundleDetailScreen
import app.reseam.manager.ui.bundles.BundleDetailViewModel
import app.reseam.manager.ui.bundles.BundlesScreen
import app.reseam.manager.ui.bundles.BundlesViewModel
import app.reseam.manager.ui.components.DetailPlaceholder
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.NavigationRail
import app.reseam.manager.ui.components.PaneRole
import app.reseam.manager.ui.components.RailDestination
import app.reseam.manager.ui.home.HomeScreen
import app.reseam.manager.ui.home.HomeViewModel
import app.reseam.manager.ui.patches.PatchesScreen
import app.reseam.manager.ui.patches.PatchesViewModel
import app.reseam.manager.ui.permissions.PermissionsScreen
import app.reseam.manager.ui.pick.PickAppScreen
import app.reseam.manager.ui.pick.PickAppViewModel
import app.reseam.manager.ui.run.RunScreen
import app.reseam.manager.ui.run.RunViewModel
import app.reseam.manager.ui.settings.SettingsScreen
import app.reseam.manager.ui.settings.SettingsViewModel
import app.reseam.manager.ui.theme.ReseamTheme
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val NavStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Home::class)
            subclass(Route.PickApp::class)
            subclass(Route.Patches::class)
            subclass(Route.Run::class)
            subclass(Route.AppDetail::class)
            subclass(Route.Bundles::class)
            subclass(Route.BundleDetail::class)
            subclass(Route.Settings::class)
            subclass(Route.Permissions::class)
        }
    }
}

private val RailDestinations = Section.entries.map { RailDestination(it, it.label, it.icon) }

/**
 * The route table and the shell around it. Wide windows keep a rail beside the content and show a
 * list route next to the detail it opened; the patch flow always has the window to itself.
 */
@Composable
fun AppNavigation(versionLabel: String, permissions: Permissions?, initialStack: List<Route>, notices: @Composable () -> Unit) {
    val graph = LocalAppGraph.current
    val layout = ReseamTheme.layout
    val backStack = rememberNavBackStack(NavStateConfiguration, *initialStack.toTypedArray())
    val motion = ReseamTheme.motion

    fun push(route: Route) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun open(section: Section) {
        backStack.clear()
        backStack.add(Route.Home)
        if (section != Section.Home) backStack.add(section.root)
    }

    val forward = remember(motion) {
        slideInHorizontally(motion.tweenBase()) { it / 8 } + fadeIn(motion.tweenBase()) togetherWith
            slideOutHorizontally(motion.tweenFast()) { -it / 12 } + fadeOut(motion.tweenFast())
    }
    val backward = remember(motion) {
        slideInHorizontally(motion.tweenBase()) { -it / 12 } + fadeIn(motion.tweenBase()) togetherWith
            slideOutHorizontally(motion.tweenFast()) { it / 8 } + fadeOut(motion.tweenFast())
    }
    val twoPane = remember {
        TwoPaneSceneStrategy<NavKey> { listKey ->
            when (listKey) {
                Route.Bundles -> DetailPlaceholder(Icons.Puzzle, "Bundles", "Select a bundle to see its patches and signer.")
                else -> DetailPlaceholder(Icons.Smartphone, "Your patched apps", "Select an app to see what was applied, or start a new patch.")
            }
        }
    }

    val current = backStack.lastOrNull() as? Route
    val section = current?.section
    val sectionBack: (() -> Unit)? = if (layout.rail) null else ::pop
    Row(Modifier.fillMaxSize()) {
        if (layout.rail && section != null) NavigationRail(RailDestinations, selected = section, onSelect = ::open)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            notices()
            Box(Modifier.weight(1f)) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { pop() },
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                    sceneStrategies = if (layout.twoPane) listOf(twoPane) else emptyList(),
                    transitionSpec = { forward },
                    popTransitionSpec = { backward },
                    predictivePopTransitionSpec = { backward },
                    entryProvider = entryProvider {
                        entry<Route.Home>(metadata = paneRole(PaneRole.List)) {
                            HomeScreen(
                                viewModel = viewModel { HomeViewModel(graph) },
                                selectedPackage = (current as? Route.AppDetail)?.packageName,
                                showSectionActions = !layout.rail,
                                onNewPatch = { push(Route.PickApp) },
                                onOpenApp = { push(Route.AppDetail(it.packageName)) },
                                onBundles = { push(Route.Bundles) },
                                onSettings = { push(Route.Settings) },
                            )
                        }
                        entry<Route.PickApp> {
                            PickAppScreen(
                                viewModel = viewModel { PickAppViewModel(graph) },
                                onBack = ::pop,
                                onContinue = { push(Route.Patches(it)) },
                            )
                        }
                        entry<Route.Patches> { route ->
                            PatchesScreen(
                                viewModel = viewModel { PatchesViewModel(graph, route.target) },
                                appName = route.target.name,
                                onBack = ::pop,
                                onRun = { target, selection, queue, bundlePaths -> push(Route.Run(target, selection, queue, bundlePaths)) },
                            )
                        }
                        entry<Route.Run> { route ->
                            RunScreen(
                                viewModel = viewModel { RunViewModel(graph, route.target, route.selection, route.bundlePaths) },
                                target = route.target,
                                queue = route.queue,
                                artifactActionLabel = graph.artifactAction.label,
                                onDone = { open(Section.Home) },
                            )
                        }
                        entry<Route.AppDetail>(metadata = paneRole(PaneRole.Detail)) { route ->
                            AppDetailScreen(
                                viewModel = viewModel { AppDetailViewModel(graph, route.packageName) },
                                onBack = ::pop,
                                onRepatch = { push(Route.Patches(it)) },
                            )
                        }
                        entry<Route.Bundles>(metadata = paneRole(PaneRole.List)) {
                            BundlesScreen(
                                viewModel = viewModel { BundlesViewModel(graph) },
                                selectedId = (current as? Route.BundleDetail)?.id,
                                onBack = sectionBack,
                                onOpen = { push(Route.BundleDetail(it.id)) },
                            )
                        }
                        entry<Route.BundleDetail>(metadata = paneRole(PaneRole.Detail)) { route ->
                            BundleDetailScreen(viewModel = viewModel { BundleDetailViewModel(graph, route.id) }, onBack = ::pop)
                        }
                        entry<Route.Settings> {
                            SettingsScreen(
                                viewModel = viewModel { SettingsViewModel(graph) },
                                versionLabel = versionLabel,
                                onBack = sectionBack,
                                onBundles = { push(Route.Bundles) },
                                onPermissions = permissions?.let { { push(Route.Permissions) } },
                            )
                        }
                        entry<Route.Permissions> {
                            PermissionsScreen(
                                permissions = checkNotNull(permissions) { "Permissions route is only reachable where the platform gates them" },
                                onBack = if (backStack.size > 1) ::pop else null,
                                onContinue = { if (backStack.size > 1) pop() else open(Section.Home) },
                            )
                        }
                    },
                )
            }
        }
    }
}
