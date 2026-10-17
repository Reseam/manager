package app.reseam.manager.platform

data class DeviceProfile(val model: String, val abis: List<String>, val sdk: Int, val densityDpi: Int)
