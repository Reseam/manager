package app.reseam.manager.platform

/** [abis] is in preference order. */
data class DeviceProfile(val abis: List<String>, val sdk: Int, val densityDpi: Int)
