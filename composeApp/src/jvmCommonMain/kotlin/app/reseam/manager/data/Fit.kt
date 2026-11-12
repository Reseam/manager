package app.reseam.manager.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Who a build is for: the phone patching it, or any phone the APK is passed on to. */
@Serializable
enum class Fit {
    @SerialName("device")
    Device,

    @SerialName("any")
    Any,
}
