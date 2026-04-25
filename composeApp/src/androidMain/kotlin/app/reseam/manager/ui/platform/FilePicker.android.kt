package app.reseam.manager.ui.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.reseam.manager.data.platform.AndroidFilePicker
import app.reseam.manager.domain.sources.FilePicker

@Composable
actual fun rememberPlatformFilePicker(): FilePicker {
    val context = LocalContext.current
    lateinit var picker: AndroidFilePicker

    val apkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picker.onApkPicked(uri)
    }
    val bundleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        picker.onBundlePicked(uri)
    }

    picker = remember(context, apkLauncher, bundleLauncher) {
        AndroidFilePicker(context, apkLauncher, bundleLauncher)
    }

    return picker
}
