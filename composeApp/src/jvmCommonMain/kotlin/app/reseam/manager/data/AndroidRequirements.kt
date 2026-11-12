package app.reseam.manager.data

import app.reseam.manager.platform.DeviceProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** The lowest Android each app version needs, by package and then version; a release never changes it, so it is kept. */
@Serializable
data class AndroidRequirementLibrary(val sdks: Map<String, Map<String, Int>> = emptyMap()) {
    fun sdk(packageName: String, versionName: String): Int? = sdks[packageName]?.get(versionName)
}

class AndroidRequirements(
    private val store: JsonStore<AndroidRequirementLibrary>,
    private val downloader: Downloader,
    private val device: DeviceProfile?,
) {
    val known: Flow<AndroidRequirementLibrary> = store.state

    private val lock = Mutex()

    /** Versions that run on the device first, so the first is the one to recommend; without a device, as they are. */
    fun fit(packageName: String, options: List<VersionOption>, known: AndroidRequirementLibrary): List<VersionOption> {
        val device = device ?: return options
        return options.map { option ->
            val sdk = option.versionName?.let { known.sdk(packageName, it) }
            option.copy(
                android = when {
                    sdk == null -> AndroidFit.Unknown
                    sdk <= device.sdk -> AndroidFit.Runs
                    else -> AndroidFit.Needs(androidName(sdk))
                },
            )
        }.sortedBy { it.android is AndroidFit.Needs }
    }

    /** One version the service can't answer for stays unknown; the others are still asked. */
    suspend fun check(packageName: String, versionNames: List<String>) {
        if (device == null) return
        lock.withLock {
            for (versionName in versionNames) {
                if (store.state.value.sdk(packageName, versionName) != null) continue
                val builds = try {
                    downloader.builds(packageName, versionName).builds
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    continue
                }
                record(packageName, versionName, builds)
            }
        }
    }

    suspend fun record(packageName: String, versionName: String, builds: List<Build>) {
        val sdk = builds.requiredSdk(device ?: return) ?: return
        store.update { it.copy(sdks = it.sdks + (packageName to it.sdks[packageName].orEmpty() + (versionName to sdk))) }
    }
}
