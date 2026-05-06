package app.reseam.manager.data.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import app.reseam.manager.domain.sources.FilePicker
import app.reseam.manager.domain.sources.PickedFile
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidFilePicker(
    private val context: Context,
    private val apkLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    private val bundleLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
) : FilePicker {
    private var pendingApk: CompletableDeferred<Uri?>? = null
    private var pendingBundle: CompletableDeferred<Uri?>? = null

    override suspend fun pickApk(): PickedFile? = pick(::pendingApk, apkLauncher, "apk-inputs", APK_MIME, ANY_MIME)

    override suspend fun pickPatchBundle(): PickedFile? = pick(::pendingBundle, bundleLauncher, "bundles", ANY_MIME)

    fun onApkPicked(uri: Uri?) {
        pendingApk?.complete(uri)
        pendingApk = null
    }

    fun onBundlePicked(uri: Uri?) {
        pendingBundle?.complete(uri)
        pendingBundle = null
    }

    private suspend fun pick(
        slot: kotlin.reflect.KMutableProperty0<CompletableDeferred<Uri?>?>,
        launcher: ManagedActivityResultLauncher<Array<String>, Uri?>,
        directoryName: String,
        vararg mimeTypes: String,
    ): PickedFile? {
        slot.get()?.cancel()
        val deferred = CompletableDeferred<Uri?>()
        slot.set(deferred)
        launcher.launch(arrayOf(*mimeTypes))
        val uri = deferred.await() ?: return null
        return withContext(Dispatchers.IO) { uri.copyToCache(directoryName) }
    }

    private fun Uri.copyToCache(directoryName: String): PickedFile {
        val displayName = displayName().sanitizeFileName()
        val target = File(context.cacheDir, "reseam/$directoryName/$displayName")
            .also { it.parentFile?.mkdirs() }

        context.contentResolver.openInputStream(this).use { input ->
            requireNotNull(input) { "Could not open selected file" }
            target.outputStream().use { output -> input.copyTo(output) }
        }

        return PickedFile(displayName = displayName, path = target.absolutePath)
    }

    private fun Uri.displayName(): String {
        context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return lastPathSegment ?: "selected-file"
    }

    private fun String.sanitizeFileName(): String =
        replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "selected-file" }

    private companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val ANY_MIME = "*/*"
    }
}
