package app.reseam.manager.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import app.reseam.manager.data.Failure
import app.reseam.sdk.InstalledApp
import app.reseam.sdk.MountNamespace
import app.reseam.sdk.MountRequest
import app.reseam.sdk.PatchedSplit
import app.reseam.sdk.Problem
import app.reseam.sdk.RootScript
import app.reseam.sdk.SdkError
import app.reseam.sdk.dexMetadata
import app.reseam.sdk.installScript
import app.reseam.sdk.installedApp
import app.reseam.sdk.isMounted as reportsMounted
import app.reseam.sdk.mountQueryScript
import app.reseam.sdk.mountScript
import app.reseam.sdk.mountStatusScript
import app.reseam.sdk.originalApkScript
import app.reseam.sdk.unmountScript
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs the SDK's mount scripts in a root shell on the device. */
class RootAppMounter(private val context: Context) : AppMounter {
    override suspend fun requestAccess(): Boolean =
        runCatching { root(RootScript("id -u\n", MountNamespace.GLOBAL)).trim() == "0" }.getOrDefault(false)

    override suspend fun install(packageName: String, apks: ApkSet) {
        // pm reports a refused install on stdout, so its output is the result either way.
        val output = withDexMetadata(apks.base, File(apks.base).nameWithoutExtension + ".dm") { metadata ->
            root(installScript(packageName, listOf(apks.base) + apks.splits.values + listOfNotNull(metadata?.path)))
        }
        when {
            "Success" in output -> Unit
            "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in output -> throw Failure.OriginalSignedDifferently()
            else -> throw Failure.OriginalNotInstalled(output.trim().lines().first())
        }
    }

    override suspend fun mount(packageName: String, apks: ApkSet) {
        val installed = installed(packageName)
        val label = label(packageName)
        withDexMetadata(apks.base, installed.dexMetadataFile) { metadata ->
            val request = MountRequest(
                packageName = packageName,
                label = label,
                versionCode = apks.versionCode,
                installed = installed,
                base = apks.base,
                splits = apks.splits.map { (name, path) -> PatchedSplit(name, path) },
                dexMetadata = metadata?.path,
            )
            root(mountScript(request))
        }
        if (!isMounted(packageName)) throw Failure.MountFailed(label)
    }

    override suspend fun unmount(packageName: String) {
        root(unmountScript(packageName))
    }

    override suspend fun isMounted(packageName: String): Boolean = reportsMounted(root(mountStatusScript(packageName)))

    override suspend fun copyOriginals(packageName: String, directory: PlatformFile): List<PlatformFile> {
        val installed = installed(packageName)
        val target = File(directory.path)
        withContext(Dispatchers.IO) {
            target.deleteRecursively()
            target.mkdirs()
        }
        return (listOf(installed.baseFile) + installed.splits.map { it.file }).map { file ->
            val copy = File(target, file)
            withContext(Dispatchers.IO) { copy.outputStream().use { output -> root(originalApkScript(packageName, file)) { it.copyTo(output) } } }
            PlatformFile(copy)
        }
    }

    private suspend fun installed(packageName: String): InstalledApp =
        installedApp(root(mountQueryScript(packageName))) ?: throw SdkError(Problem.AppNotInstalled(packageName), "$packageName is not installed")

    private fun label(packageName: String): String {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(packageName, 0)
        }
        return context.packageManager.getApplicationLabel(info).toString()
    }

    /** Runs [block] with the dex metadata for [apk] written to the cache as [name], or null when there is none or no [name]. */
    private suspend fun <T> withDexMetadata(apk: String, name: String?, block: suspend (File?) -> T): T {
        val metadata = name?.let {
            withContext(Dispatchers.IO) {
                dexMetadata(apk, Build.VERSION.SDK_INT.toUInt())?.let { bytes ->
                    File(context.cacheDir, "dex-metadata").apply { mkdirs() }.resolve(name).apply { writeBytes(bytes) }
                }
            }
        }
        return try {
            block(metadata)
        } finally {
            metadata?.delete()
        }
    }

    private suspend fun root(script: RootScript): String = root(script) { it.readBytes().decodeToString() }

    private suspend fun <T> root(script: RootScript, read: (InputStream) -> T): T = withContext(Dispatchers.IO) {
        val shell = when (script.namespace) {
            MountNamespace.GLOBAL -> "sh"
            MountNamespace.PRIVATE -> "unshare -m sh"
        }
        val process = ProcessBuilder("su", "--mount-master", "-c", shell).start()
        process.outputStream.bufferedWriter().use { it.write(script.script) }
        val result = process.inputStream.use(read)
        val errors = process.errorStream.bufferedReader().use { it.readText() }
        if (process.waitFor() != 0) throw Failure.RootCommandFailed(errors.trim())
        result
    }
}
