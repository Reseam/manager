package app.reseam.manager.data

import app.reseam.manager.platform.DeviceProfile

private val UniversalArchitectures = setOf("universal", "noarch")
private val MinAndroid = Regex("""Android ([\d.]+[LW]?)\+""")
private val DpiRange = Regex("""(\d+)(?:-(\d+))?dpi""")

// Android loads resources from the nearest standard density at or above the screen's, so a 440dpi phone runs 480dpi builds.
private val DensityBuckets = listOf(120, 160, 213, 240, 320, 480, 640)

private val ApiLevels = mapOf(
    "1.0" to 1, "1.1" to 2, "1.5" to 3, "1.6" to 4, "2.0" to 5, "2.0.1" to 6, "2.1" to 7, "2.2" to 8, "2.3" to 9, "2.3.3" to 10,
    "3.0" to 11, "3.1" to 12, "3.2" to 13, "4.0" to 14, "4.0.3" to 15, "4.1" to 16, "4.2" to 17, "4.3" to 18, "4.4" to 19, "4.4W" to 20,
    "5.0" to 21, "5.1" to 22, "6.0" to 23, "7.0" to 24, "7.1" to 25, "8.0" to 26, "8.1" to 27, "9" to 28, "10" to 29, "11" to 30,
    "12" to 31, "12L" to 32, "13" to 33, "14" to 34, "15" to 35, "16" to 36,
)

val Build.minSdk: Int? get() = MinAndroid.find(minAndroid)?.groupValues?.get(1)?.let(ApiLevels::get)

/** Unrecognized minimum-version or density labels don't rule a build out; only what can be read does. */
fun Build.fits(device: DeviceProfile): Boolean {
    val architecture = abis.any { it in UniversalArchitectures || it in device.abis }
    val bucket = DensityBuckets.firstOrNull { it >= device.densityDpi } ?: DensityBuckets.last()
    val density = dpi == "nodpi" || DpiRange.matchEntire(dpi)?.destructured?.let { (low, high) ->
        val covered = low.toInt()..high.ifEmpty { low }.toInt()
        device.densityDpi in covered || bucket in covered
    } ?: true
    return architecture && density && (minSdk ?: 0) <= device.sdk
}

fun List<Build>.best(device: DeviceProfile?): Build? =
    filter { device == null || it.fits(device) }.minWithOrNull(
        compareBy<Build> { it.container != BuildContainer.Apk }
            .thenBy { build -> device?.abis?.firstOrNull()?.let { it !in build.abis } ?: true }
            .thenBy { build -> build.abis.none { it in UniversalArchitectures } }
            .thenBy { it.dpi != "nodpi" },
    )
