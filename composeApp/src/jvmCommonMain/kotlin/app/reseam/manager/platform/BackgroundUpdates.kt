package app.reseam.manager.platform

/** Checks for updates on a schedule while Manager is closed. */
interface BackgroundUpdates {
    fun schedule()
}
