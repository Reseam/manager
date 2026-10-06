package app.reseam.manager.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.path
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bind-mounts patched APKs over the installed ones from a root module, which Magisk, KernelSU, and APatch list
 * and let the user turn off. The module's boot script mounts again after every reboot while the installed version
 * still matches, and mounting now runs that same script.
 */
class RootAppMounter(private val context: Context) : AppMounter {
    override val available: Boolean
        get() = System.getenv("PATH").orEmpty().split(':').any { File(it, "su").exists() }

    override suspend fun requestAccess(): Boolean = runCatching { root("id -u").trim() == "0" }.getOrDefault(false)

    override suspend fun mount(packageName: String, artifact: PlatformFile) {
        val installed = installed(packageName)
        val apks = withContext(Dispatchers.IO) {
            if (artifact.isDirectory()) artifact.list().map { File(it.path) }.filter { it.extension == "apk" }.associateBy { it.name }
            else mapOf(BASE_APK to File(artifact.path))
        }
        check(apks.keys == installed.files) {
            "The patched ${installed.label} has different parts than the installed one. Patch the installed app again to mount it."
        }
        check(versionCode(apks.getValue(BASE_APK)) == installed.versionCode) {
            "${installed.label} changed since it was patched. Patch it again to mount it."
        }
        root(installScript(packageName, installed, apks))
        check(isMounted(packageName)) { "${installed.label} did not mount." }
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

    private fun installScript(packageName: String, installed: InstalledPackage, apks: Map<String, File>): String {
        val dir = moduleDirectory(packageName)
        return buildString {
            appendLine("set -e")
            appendLine(script("dir=$dir"))
            appendLine("[ -d \"\$dir\" ] && unmount $packageName \"\$dir\"")
            appendLine("rm -rf \"\$dir\"")
            appendLine("mkdir -p \"\$dir/apk\"")
            apks.forEach { (name, file) -> appendLine("cp ${quote(file.path)} \"\$dir/apk/$name\"") }
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

    private fun installed(packageName: String): InstalledPackage {
        val info = packageInfo(packageName) ?: error("$packageName is not installed.")
        val app = checkNotNull(info.applicationInfo) { "$packageName has no application info." }
        return InstalledPackage(
            label = context.packageManager.getApplicationLabel(app).toString(),
            versionName = info.versionName.orEmpty(),
            versionCode = info.longVersionCode,
            files = (listOf(app.sourceDir) + app.splitSourceDirs.orEmpty()).map { File(it).name }.toSet(),
        )
    }

    private fun versionCode(apk: File): Long =
        checkNotNull(context.packageManager.getPackageArchiveInfo(apk.path, 0)) { "${apk.name} is not a readable APK." }.longVersionCode

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

    /**
     * Runs [script] as root from the global mount namespace, so a mount reaches every app's processes. [shell] reads
     * the script; [read] consumes its output.
     */
    private suspend fun <T> root(script: String, shell: String, read: (InputStream) -> T): T = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("su", "--mount-master", "-c", shell).start()
        process.outputStream.bufferedWriter().use { it.write(script) }
        val result = process.inputStream.use(read)
        val errors = process.errorStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "The root command failed: ${errors.trim()}" }
        result
    }

    private class InstalledPackage(val label: String, val versionName: String, val versionCode: Long, val files: Set<String>)

    private companion object {
        const val BASE_APK = "base.apk"
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

        /** The shared functions, then [body]. */
        fun script(body: String) = FUNCTIONS + "\n" + body.trimIndent()

        fun moduleId(packageName: String) = "reseam-$packageName"

        fun moduleDirectory(packageName: String) = "/data/adb/modules/${moduleId(packageName)}"

        fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"
    }
}
