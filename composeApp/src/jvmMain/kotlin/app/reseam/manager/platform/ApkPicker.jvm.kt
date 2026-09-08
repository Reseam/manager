package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
actual fun rememberApkPicker(onResult: (Result<PlatformFile?>) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            val result = try {
                Result.success(FileKit.openFilePicker(type = FileKitType.File(listOf("apk", "apkm", "xapk"))))
            } catch (cancelled: CancellationException) {
                onResult(Result.success(null))
                throw cancelled
            } catch (error: Exception) {
                Result.failure(error)
            }
            onResult(result)
        }
    }
}
