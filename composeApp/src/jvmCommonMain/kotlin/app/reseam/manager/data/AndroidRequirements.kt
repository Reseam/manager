package app.reseam.manager.data

import app.reseam.manager.platform.DeviceProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

/** The lowest API level a version needs on this device, and the Android release it stands for, such as `8.1`. */
@Serializable
data class AndroidRequirement(val sdk: Int, val android: String)

/** What each app version needs, by package and then version; a release never changes it, so it is kept. */
@Serializable
data class AndroidRequirementLibrary(val versions: Map<String, Map<String, AndroidRequirement>> = emptyMap()) {
    fun of(packageName: String, versionName: String): AndroidRequirement? = versions[packageName]?.get(versionName)
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
            val requirement = option.versionName?.let { known.of(packageName, it) }
            option.copy(
                android = when {
                    requirement == null -> AndroidFit.Unknown
                    requirement.sdk <= device.sdk -> AndroidFit.Runs
                    else -> AndroidFit.Needs(requirement.android)
                },
            )
        }.sortedBy { it.android is AndroidFit.Needs }
    }

    /** One version the service can't answer for stays unknown; the others are still asked. */
    suspend fun check(packageName: String, versionNames: List<String>) {
        if (device == null) return
        lock.withLock {
            for (versionName in versionNames) {
                if (store.state.value.of(packageName, versionName) != null) continue
                val builds = try {
                    downloader.builds(packageName, versionName, Fit.Device, device)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    continue
                }
                record(packageName, versionName, builds)
            }
        }
    }

    /** The service picks a build that runs when one does, and otherwise the one that needs the lowest Android. */
    suspend fun record(packageName: String, versionName: String, builds: AppBuilds) {
        if (device == null) return
        val build = builds.chosen ?: return
        val requirement = AndroidRequirement(build.sdk ?: return, build.android ?: return)
        store.update { it.copy(versions = it.versions + (packageName to it.versions[packageName].orEmpty() + (versionName to requirement))) }
    }
}
