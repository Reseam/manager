package app.reseam.manager.sdk

import app.reseam.sdk.ApkInspection
import app.reseam.sdk.ApplicationIcon

internal actual fun openApkArchive(path: String): ApkArchive = NativeApkArchive(ApkInspection(path, emptyList()))

private class NativeApkArchive(private val inspection: ApkInspection) : ApkArchive {
    override val metadata: ApkMetadata by lazy { WireJson.decodeFromString(inspection.metadataJson()) }
    override val basePath: String get() = inspection.basePath()
    override val splitPaths: List<String> get() = inspection.splitPaths()

    override fun icon(): ApkIcon? = when (val icon = inspection.applicationIcon()) {
        null -> null
        is ApplicationIcon.Bitmap -> ApkIcon.Bitmap(icon.value0)
        is ApplicationIcon.Adaptive -> ApkIcon.Adaptive(icon.background.toLayer(), icon.foreground.toLayer())
    }

    override fun close() = inspection.close()
}

private fun app.reseam.sdk.IconLayer.toLayer(): IconLayer = when (this) {
    is app.reseam.sdk.IconLayer.Bitmap -> IconLayer.Bitmap(value0)
    is app.reseam.sdk.IconLayer.Color -> IconLayer.Color(value0.toInt())
}
