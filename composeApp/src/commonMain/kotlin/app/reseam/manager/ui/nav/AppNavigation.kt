package app.reseam.manager.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import app.reseam.manager.ui.home.HomeScreen
import app.reseam.manager.ui.home.HomeViewModel
import app.reseam.manager.ui.patches.PatchesScreen
import app.reseam.manager.ui.permissions.PermissionsScreen
import app.reseam.manager.ui.patches.PatchesViewModel
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

@Composable
fun AppNavigation(versionLabel: String, permissions: Permissions?, startRoute: Route) {
    val graph = LocalAppGraph.current
    val backStack = rememberNavBackStack(NavStateConfiguration, startRoute)
    val motion = ReseamTheme.motion

    fun push(route: Route) {
        if (backStack.lastOrNull() != route) backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun home() {
        backStack.clear()
        backStack.add(Route.Home)
    }

    val forward = remember(motion) {
        slideInHorizontally(motion.tweenBase()) { it / 8 } + fadeIn(motion.tweenBase()) togetherWith
            slideOutHorizontally(motion.tweenFast()) { -it / 12 } + fadeOut(motion.tweenFast())
    }
    val backward = remember(motion) {
        slideInHorizontally(motion.tweenBase()) { -it / 12 } + fadeIn(motion.tweenBase()) togetherWith
            slideOutHorizontally(motion.tweenFast()) { it / 8 } + fadeOut(motion.tweenFast())
    }

    NavDisplay(
        backStack = backStack,
        onBack = { pop() },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        transitionSpec = { forward },
        popTransitionSpec = { backward },
        predictivePopTransitionSpec = { backward },
        entryProvider = entryProvider {
            entry<Route.Home> {
                HomeScreen(
                    viewModel = viewModel { HomeViewModel(graph) },
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
                    onRun = { target, selection, queue -> push(Route.Run(target, selection, queue)) },
                )
            }
            entry<Route.Run> { route ->
                RunScreen(
                    viewModel = viewModel { RunViewModel(graph, route.target, route.selection) },
                    target = route.target,
                    queue = route.queue,
                    artifactActionLabel = graph.artifactAction.label,
                    onDone = ::home,
                )
            }
            entry<Route.AppDetail> { route ->
                AppDetailScreen(
                    viewModel = viewModel { AppDetailViewModel(graph, route.packageName) },
                    onBack = ::pop,
                    onRepatch = { push(Route.Patches(it)) },
                )
            }
            entry<Route.Bundles> {
                BundlesScreen(
                    viewModel = viewModel { BundlesViewModel(graph) },
                    onBack = ::pop,
                    onOpen = { push(Route.BundleDetail(it.id)) },
                )
            }
            entry<Route.BundleDetail> { route ->
                BundleDetailScreen(viewModel = viewModel { BundleDetailViewModel(graph, route.id) }, onBack = ::pop)
            }
            entry<Route.Settings> {
                SettingsScreen(
                    viewModel = viewModel { SettingsViewModel(graph) },
                    versionLabel = versionLabel,
                    onBack = ::pop,
                    onBundles = { push(Route.Bundles) },
                    onPermissions = permissions?.let { { push(Route.Permissions) } },
                )
            }
            entry<Route.Permissions> {
                PermissionsScreen(
                    permissions = checkNotNull(permissions) { "Permissions route is only reachable where the platform gates them" },
                    onBack = if (backStack.size > 1) ::pop else null,
                    onContinue = { if (backStack.size > 1) pop() else home() },
                )
            }
        },
    )
}
