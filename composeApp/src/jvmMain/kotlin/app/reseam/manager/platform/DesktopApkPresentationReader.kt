package app.reseam.manager.platform

import app.reseam.manager.sdk.ApkArchive
import app.reseam.sdk.ApplicationIcon
import app.reseam.sdk.IconLayer
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

private fun encode(icon: ApplicationIcon): ByteArray = when (icon) {
    is ApplicationIcon.Bitmap -> icon.field0
    is ApplicationIcon.Adaptive -> Surface.makeRasterN32Premul(IconSize, IconSize).use { surface ->
        surface.canvas.draw(icon.background)
        surface.canvas.draw(icon.foreground)
        surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)!!.bytes
    }
}

private fun Canvas.draw(layer: IconLayer) {
    when (layer) {
        is IconLayer.Color -> drawPaint(Paint().apply { color = layer.field0.toInt() })
        is IconLayer.Bitmap -> {
            val side = IconSize * AdaptiveCanvasOverVisible
            val offset = (IconSize - side) / 2
            Image.makeFromEncoded(layer.field0).use { image ->
                drawImageRect(image, Rect.makeWH(image.width.toFloat(), image.height.toFloat()), Rect.makeXYWH(offset, offset, side, side), SamplingMode.MITCHELL, null, true)
            }
        }
    }
}
