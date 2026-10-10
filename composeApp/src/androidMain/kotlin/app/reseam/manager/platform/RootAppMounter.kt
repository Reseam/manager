package app.reseam.manager.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import app.reseam.manager.data.Failure
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootAppMounter(private val context: Context) : AppMounter {
    override suspend fun requestAccess(): Boolean = runCatching { root("id -u").trim() == "0" }.getOrDefault(false)

    override suspend fun matches(packageName: String, apks: ApkSet): Boolean = installedOrNull(packageName)?.matches(apks) == true

    override suspend fun install(packageName: String, apks: ApkSet) {
        val files = (listOf(apks.base) + apks.splits.values).joinToString(" ", transform = ::quote)
        // pm reports a refused install on stdout, so its output is the result either way.
        val output = root(
            script(
                """
                dir=${moduleDirectory(packageName)}
                [ -d "${'$'}dir" ] && unmount $packageName "${'$'}dir"
                pm install -r -d $files 2>&1 || true
                """,
            ),
        )
        when {
            "Success" in output -> Unit
            "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in output -> throw Failure.OriginalSignedDifferently()
            else -> throw Failure.OriginalNotInstalled(output.trim().lines().first())
        }
    }

    override suspend fun mount(packageName: String, apks: ApkSet) {
        val installed = installed(packageName)
        if (!installed.matches(apks)) throw Failure.InstalledVersionDiffers(installed.label)
        val files = mapOf(installed.baseFile to apks.base) + installed.splitFiles.map { (split, file) -> file to apks.splits.getValue(split) }
        root(installScript(packageName, installed, files))
        if (!isMounted(packageName)) throw Failure.MountFailed(installed.label)
    }

    override suspend fun unmount(packageName: String) {
        root(
            script(
                """
                set -e
                dir=${moduleDirectory(packageName)}
                unmount $packageName "${'$'}dir"
                rm -rf "${'$'}dir"
                am force-stop $packageName
                """,
            ),
        )
    }

    override suspend fun isMounted(packageName: String): Boolean =
        root(script("mounted $packageName ${moduleDirectory(packageName)} && echo yes || echo no")).trim() == "yes"

    override suspend fun copyOriginals(packageName: String, directory: PlatformFile): List<PlatformFile> {
        val app = checkNotNull(packageInfo(packageName)?.applicationInfo) { "$packageName is not installed." }
        val target = File(directory.path)
        withContext(Dispatchers.IO) {
            target.deleteRecursively()
            target.mkdirs()
        }
        return (listOf(app.sourceDir) + app.splitSourceDirs.orEmpty()).map { path ->
            val copy = File(target, File(path).name)
            // The mount comes off in a private namespace only, so the app keeps running patched meanwhile.
            val script = script(
                """
                set -e
                mount -o rprivate none /
                unmount $packageName ${moduleDirectory(packageName)}
                cat ${quote(path)}
                """,
            )
            withContext(Dispatchers.IO) { copy.outputStream().use { output -> root(script, PRIVATE_SHELL) { it.copyTo(output) } } }
            PlatformFile(copy)
        }
    }

    private fun installScript(packageName: String, installed: InstalledPackage, files: Map<String, String>): String {
        val dir = moduleDirectory(packageName)
        return buildString {
            appendLine("set -e")
            appendLine(script("dir=$dir"))
            appendLine("[ -d \"\$dir\" ] && unmount $packageName \"\$dir\"")
            appendLine("rm -rf \"\$dir\"")
            appendLine("mkdir -p \"\$dir/apk\"")
            files.forEach { (name, path) -> appendLine("cp ${quote(path)} \"\$dir/apk/$name\"") }
            appendLine("chown 1000:1000 \"\$dir/apk/\"*")
            appendLine("chmod 644 \"\$dir/apk/\"*")
            appendLine("chcon u:object_r:apk_data_file:s0 \"\$dir/apk/\"*")
            appendLine("cat > \"\$dir/module.prop\" <<'RESEAM_EOF'")
            appendLine("id=${moduleId(packageName)}")
            appendLine("name=Reseam: ${installed.label}")
            appendLine("version=${installed.versionName}")
            appendLine("versionCode=${installed.versionCode}")
            appendLine("author=Reseam Manager")
            appendLine("description=Mounts the patched ${installed.label} over the installed app.")
            appendLine("RESEAM_EOF")
            appendLine("cat > \"\$dir/service.sh\" <<'RESEAM_EOF'")
            appendLine(bootScript(packageName, installed.versionCode))
            appendLine("RESEAM_EOF")
            appendLine("chmod 755 \"\$dir/service.sh\"")
            appendLine("sh \"\$dir/service.sh\"")
        }
    }

    private fun bootScript(packageName: String, versionCode: Long) =
        "#!/system/bin/sh\n" + script(
            """
            until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 1; done
            [ "$(dumpsys package $packageName | sed -n 's/.*versionCode=\([0-9]*\).*/\1/p' | head -n 1)" = $versionCode ] || exit 0
            for path in $(installed_paths $packageName); do
                file="${'$'}{0%/*}/apk/${'$'}{path##*/}"
                [ -f "${'$'}file" ] || continue
                same_file "${'$'}path" "${'$'}file" || mount -o bind "${'$'}file" "${'$'}path"
            done
            am force-stop $packageName
            """,
        )

    private fun installed(packageName: String): InstalledPackage = installedOrNull(packageName) ?: throw Failure.NotInstalled(packageName)

    private fun installedOrNull(packageName: String): InstalledPackage? {
        val info = packageInfo(packageName) ?: return null
        val app = checkNotNull(info.applicationInfo) { "$packageName has no application info." }
        return InstalledPackage(
            label = context.packageManager.getApplicationLabel(app).toString(),
            versionName = info.versionName.orEmpty(),
            versionCode = info.longVersionCode,
            baseFile = File(app.sourceDir).name,
            splitFiles = app.splitNames.orEmpty().zip(app.splitSourceDirs.orEmpty().map { File(it).name }).toMap(),
        )
    }

    private fun packageInfo(packageName: String): PackageInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private suspend fun root(script: String): String = root(script, GLOBAL_SHELL) { it.readBytes().decodeToString() }

    private suspend fun <T> root(script: String, shell: String, read: (InputStream) -> T): T = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("su", "--mount-master", "-c", shell).start()
        process.outputStream.bufferedWriter().use { it.write(script) }
        val result = process.inputStream.use(read)
        val errors = process.errorStream.bufferedReader().use { it.readText() }
        if (process.waitFor() != 0) throw Failure.RootCommandFailed(errors.trim())
        result
    }

    private class InstalledPackage(
        val label: String,
        val versionName: String,
        val versionCode: Long,
        val baseFile: String,
        val splitFiles: Map<String, String>,
    ) {
        fun matches(apks: ApkSet) = versionCode == apks.versionCode && splitFiles.keys == apks.splits.keys
    }

    private companion object {
        const val GLOBAL_SHELL = "sh"
        const val PRIVATE_SHELL = "unshare -m sh"

        // A bind mount shares the device and inode of its source, even where the root manager hides mounts.
        val FUNCTIONS =
            """
            installed_paths() { pm path "${'$'}1" | sed 's/^package://'; }
            same_file() { [ "$(stat -c %d:%i "${'$'}1")" = "$(stat -c %d:%i "${'$'}2")" ]; }
            mounted() {
                [ -d "${'$'}2/apk" ] || return 1
                for path in $(installed_paths "${'$'}1"); do
                    same_file "${'$'}path" "${'$'}2/apk/${'$'}{path##*/}" || return 1
                done
            }
            unmount() {
                for path in $(installed_paths "${'$'}1"); do
                    file="${'$'}2/apk/${'$'}{path##*/}"
                    while [ -f "${'$'}file" ] && same_file "${'$'}path" "${'$'}file"; do umount -l "${'$'}path"; done
                done
            }
            """.trimIndent()

        fun script(body: String) = FUNCTIONS + "\n" + body.trimIndent()

        fun moduleId(packageName: String) = "reseam-$packageName"

        fun moduleDirectory(packageName: String) = "/data/adb/modules/${moduleId(packageName)}"

        fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"
    }
}
