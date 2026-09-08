package app.reseam.manager.platform

import app.reseam.manager.sdk.ApkArchive
import app.reseam.manager.sdk.ApkIcon
import app.reseam.manager.sdk.IconLayer
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

private const val IconSize = 192
private const val AdaptiveCanvasOverVisible = 108f / 72f

/** Bitmaps pass through; adaptive icons are composited the way a launcher does, background under foreground, cropped to the visible area. */
object DesktopApkPresentationReader : ApkPresentationReader {
    override fun read(archive: ApkArchive) = AppPresentation(archive.metadata.applicationLabel, archive.icon()?.let(::encode))
}

private fun encode(icon: ApkIcon): ByteArray = when (icon) {
    is ApkIcon.Bitmap -> icon.bytes
    is ApkIcon.Adaptive -> Surface.makeRasterN32Premul(IconSize, IconSize).use { surface ->
        surface.canvas.draw(icon.background)
        surface.canvas.draw(icon.foreground)
        surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes
    }
}

private fun Canvas.draw(layer: IconLayer) {
    when (layer) {
        is IconLayer.Color -> drawPaint(Paint().apply { color = layer.argb })
        is IconLayer.Bitmap -> {
            val side = IconSize * AdaptiveCanvasOverVisible
            val offset = (IconSize - side) / 2
            Image.makeFromEncoded(layer.bytes).use { image ->
                drawImageRect(image, Rect.makeWH(image.width.toFloat(), image.height.toFloat()), Rect.makeXYWH(offset, offset, side, side), SamplingMode.MITCHELL, null, true)
            }
        }
    }
}
