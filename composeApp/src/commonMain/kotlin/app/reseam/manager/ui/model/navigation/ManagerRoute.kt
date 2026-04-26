package app.reseam.manager.ui.model.navigation

sealed interface ManagerRoute {
    data object Home : ManagerRoute
    data object Inputs : ManagerRoute
    data object Patches : ManagerRoute
    data object Run : ManagerRoute
    data class AppDetail(val appId: String) : ManagerRoute
    data object Settings : ManagerRoute
    data object Bundles : ManagerRoute
    data class BundleDetail(val bundleId: String) : ManagerRoute
    data object Permissions : ManagerRoute
}

data class ManagerNavigationState(
    val backStack: List<ManagerRoute> = listOf(ManagerRoute.Home),
) {
    val current: ManagerRoute
        get() = backStack.lastOrNull() ?: ManagerRoute.Home

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun push(route: ManagerRoute): ManagerNavigationState =
        copy(backStack = backStack + route)

    fun replace(route: ManagerRoute): ManagerNavigationState =
        copy(backStack = backStack.dropLast(1) + route)

    fun reset(route: ManagerRoute = ManagerRoute.Home): ManagerNavigationState =
        copy(backStack = listOf(route))

    fun setStack(vararg routes: ManagerRoute): ManagerNavigationState =
        copy(backStack = routes.toList().ifEmpty { listOf(ManagerRoute.Home) })

    fun pop(): ManagerNavigationState =
        if (canGoBack) copy(backStack = backStack.dropLast(1)) else this
}
