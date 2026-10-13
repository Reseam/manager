package app.reseam.manager.platform

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import app.reseam.manager.data.Failure
import app.reseam.sdk.dexMetadata
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
        // pm reports a refused install on stdout, so its output is the result either way. A session takes dex
        // metadata, which pm install would parse as one more APK.
        val output = withDexMetadata(apks.base, File(apks.base).nameWithoutExtension + ".dm") { metadata ->
            val writes = (listOf(apks.base) + apks.splits.values + listOfNotNull(metadata?.path))
                .joinToString("\n") { "pm install-write \"${'$'}session\" ${quote(File(it).name)} ${quote(it)} > /dev/null" }
            root(
                script(
                    """
                    dir=${moduleDirectory(packageName)}
                    [ -d "${'$'}dir" ] && unmount $packageName "${'$'}dir"
                    session=$(pm install-create -r -d | sed -n 's/.*\[\([0-9]*\)\].*/\1/p')
                    $writes
                    pm install-commit "${'$'}session" 2>&1 || true
                    """,
                ),
            )
        }
        when {
            "Success" in output -> Unit
            "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in output -> throw Failure.OriginalSignedDifferently()
            else -> throw Failure.OriginalNotInstalled(output.trim().lines().first())
        }
    }

    override suspend fun mount(packageName: String, apks: ApkSet) {
        val installed = installed(packageName)
        if (!installed.matches(apks)) throw Failure.InstalledVersionDiffers(installed.label)
        // ART takes the installed app's dex metadata before any other profile, so the original's has to be covered too.
        withDexMetadata(apks.base, installed.dexMetadataFile) { metadata ->
            val files = mapOf(installed.baseFile to apks.base) +
                installed.splitFiles.map { (split, file) -> file to apks.splits.getValue(split) } +
                listOfNotNull(metadata?.let { it.name to it.path })
            root(installScript(packageName, installed, files))
        }
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
                recompile $packageName
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
            appendLine("mounted $packageName \"\$dir\" && recompile $packageName")
        }
    }

    private fun bootScript(packageName: String, versionCode: Long) =
        "#!/system/bin/sh\n" + script(
            """
            until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 1; done
            [ "$(dumpsys package $packageName | sed -n 's/.*versionCode=\([0-9]*\).*/\1/p' | head -n 1)" = $versionCode ] || exit 0
            app=$(app_dir $packageName)
            for file in "${'$'}{0%/*}"/apk/*; do
                target="${'$'}app/${'$'}{file##*/}"
                [ -f "${'$'}target" ] || continue
                same_file "${'$'}target" "${'$'}file" || mount -o bind "${'$'}file" "${'$'}target"
            done
            am force-stop $packageName
            """,
        )

    private fun installed(packageName: String): InstalledPackage = installedOrNull(packageName) ?: throw Failure.NotInstalled(packageName)

    private fun installedOrNull(packageName: String): InstalledPackage? {
        val info = packageInfo(packageName) ?: return null
        val app = checkNotNull(info.applicationInfo) { "$packageName has no application info." }
        val base = File(app.sourceDir)
        return InstalledPackage(
            label = context.packageManager.getApplicationLabel(app).toString(),
            versionName = info.versionName.orEmpty(),
            versionCode = info.longVersionCode,
            baseFile = base.name,
            splitFiles = app.splitNames.orEmpty().zip(app.splitSourceDirs.orEmpty().map { File(it).name }).toMap(),
            dexMetadataFile = base.resolveSibling(base.nameWithoutExtension + ".dm").takeIf { it.exists() }?.name,
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
        val dexMetadataFile: String?,
    ) {
        fun matches(apks: ApkSet) = versionCode == apks.versionCode && splitFiles.keys == apks.splits.keys
    }

    private companion object {
        const val GLOBAL_SHELL = "sh"
        const val PRIVATE_SHELL = "unshare -m sh"

        // A bind mount shares the device and inode of its source, even where the root manager hides mounts.
        // Swapping code under ART leaves its compiled code and profiles stale, so recompile starts over as an install
        // does. Like an install, a failed compile leaves the app running from its APK, so its result is not checked.
        val FUNCTIONS =
            """
            app_dir() { dirname "$(pm path "${'$'}1" | head -n 1 | sed 's/^package://')"; }
            same_file() { [ "$(stat -c %d:%i "${'$'}1")" = "$(stat -c %d:%i "${'$'}2")" ]; }
            mounted() {
                [ -d "${'$'}2/apk" ] || return 1
                local app file
                app=$(app_dir "${'$'}1")
                for file in "${'$'}2"/apk/*; do
                    same_file "${'$'}app/${'$'}{file##*/}" "${'$'}file" || return 1
                done
            }
            unmount() {
                local app file
                app=$(app_dir "${'$'}1")
                for file in "${'$'}2"/apk/*; do
                    while [ -f "${'$'}file" ] && same_file "${'$'}app/${'$'}{file##*/}" "${'$'}file"; do umount -l "${'$'}app/${'$'}{file##*/}"; done
                done
            }
            recompile() { pm compile --reset "${'$'}1" && pm compile -r install "${'$'}1"; }
            """.trimIndent()

        fun script(body: String) = FUNCTIONS + "\n" + body.trimIndent()

        fun moduleId(packageName: String) = "reseam-$packageName"

        fun moduleDirectory(packageName: String) = "/data/adb/modules/${moduleId(packageName)}"

        fun quote(value: String) = "'" + value.replace("'", "'\\''") + "'"
    }
}
