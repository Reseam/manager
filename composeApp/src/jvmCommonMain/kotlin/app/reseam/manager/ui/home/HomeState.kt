package app.reseam.manager.ui.home

import app.reseam.manager.ui.components.AppLook

data class HomeApp(
    val look: AppLook,
    val versionName: String?,
    val patchCount: Int? = null,
    val applied: Int? = null,
    val mounted: Boolean = false,
    val saved: Boolean = false,
)

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
