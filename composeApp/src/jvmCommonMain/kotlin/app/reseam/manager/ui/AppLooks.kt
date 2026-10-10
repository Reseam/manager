package app.reseam.manager.ui

import app.reseam.manager.AppGraph
import app.reseam.manager.ui.components.AppLook
import kotlinx.coroutines.flow.first

suspend fun AppGraph.lookOf(packageName: String): AppLook {
    installedApps?.query(listOf(packageName))?.firstOrNull()?.let { return AppLook(it.name, packageName) }
    patchedApps.find(packageName).first()?.let { return AppLook(it.name, packageName, it.iconPath) }
    savedApks.apks.first().firstOrNull { it.packageName == packageName }?.let { return AppLook(it.name, packageName, it.iconPath) }
    return AppLook(packageName, packageName)
}
