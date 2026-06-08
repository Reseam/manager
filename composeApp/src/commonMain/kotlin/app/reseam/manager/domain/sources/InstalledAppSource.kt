package app.reseam.manager.domain.sources

import app.reseam.manager.ui.model.InstalledAppSummary

interface InstalledAppSource {
    /**
     * Resolves which of the given [packageNames] are installed on the device, returning a summary
     * for each match. Packages that are not installed are silently skipped.
     */
    suspend fun apps(packageNames: Collection<String>): List<InstalledAppSummary>
}
