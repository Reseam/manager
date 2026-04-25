package app.reseam.manager.ui.platform

interface PermissionHandler {
    val canInstallUnknownApps: Boolean
    val isNotificationsEnabled: Boolean
    val isBatteryOptimizationExempt: Boolean
    fun refresh()
    fun requestInstallApps()
    fun requestNotifications()
    fun requestBatteryOptimization()
}

object NoOpPermissionHandler : PermissionHandler {
    override val canInstallUnknownApps: Boolean = true
    override val isNotificationsEnabled: Boolean = true
    override val isBatteryOptimizationExempt: Boolean = true
    override fun refresh() {}
    override fun requestInstallApps() {}
    override fun requestNotifications() {}
    override fun requestBatteryOptimization() {}
}
