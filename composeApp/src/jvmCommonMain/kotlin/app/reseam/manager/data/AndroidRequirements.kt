package app.reseam.manager.data

import app.reseam.manager.platform.DeviceProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** The lowest Android each app version needs, by package and version; a release never changes it, so it is kept. */
@Serializable
data class AndroidRequirementLibrary(val sdks: Map<String, Int> = emptyMap())

private fun key(packageName: String, versionName: String) = "$packageName@$versionName"

class AndroidRequirements(
    private val store: JsonStore<AndroidRequirementLibrary>,
    private val downloader: Downloader,
    private val device: DeviceProfile?,
) {
    val known: Flow<Map<String, Int>> = store.state.map { it.sdks }

    private val lock = Mutex()

    /** Versions that run on the device first, so the first is the one to recommend; without a device, as they are. */
    fun fit(packageName: String, options: List<VersionOption>, known: Map<String, Int>): List<VersionOption> {
        val device = device ?: return options
        return options.map { option ->
            val sdk = option.versionName?.let { known[key(packageName, it)] }
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
                if (key(packageName, versionName) in store.state.value.sdks) continue
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
        store.update { it.copy(sdks = it.sdks + (key(packageName, versionName) to sdk)) }
    }
}
