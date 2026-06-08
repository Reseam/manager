package app.reseam.manager.data.platform

import app.reseam.manager.domain.sources.InstalledAppSource
import app.reseam.manager.ui.model.InstalledAppSummary

class DesktopInstalledAppSource : InstalledAppSource {
    override suspend fun apps(packageNames: Collection<String>): List<InstalledAppSummary> = emptyList()
}
