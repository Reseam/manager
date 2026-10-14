package app.reseam.manager.platform

import kotlinx.serialization.Serializable

@Serializable
data class DeviceProfile(val model: String, val abis: List<String>, val sdk: Int, val densityDpi: Int)
