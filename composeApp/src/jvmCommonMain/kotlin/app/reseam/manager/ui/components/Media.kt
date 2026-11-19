package app.reseam.manager.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.reseam.manager.data.ReleaseMedia
import app.reseam.manager.platform.Video
import app.reseam.manager.resources.*
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.window.Dialog as Window

/** The shape media holds before its own is known, and the shape of the tiles that stand in for it. */
const val MediaRatio = 16f / 9f

private val MaxPictureHeight = 480.dp
private const val MaxZoom = 5f
private const val DoubleTapZoom = 2.5f
private const val ScrimAlpha = 0.92f

/** A release's images and videos, in the order its notes list them. */
@Composable
fun ReleaseMediaList(media: List<ReleaseMedia>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        media.forEach { item ->
            key(item.url) { if (item.isVideo) Video(item, Modifier.fillMaxWidth()) else Picture(item) }
        }
    }
}

/** The first image of [media] cropped to [MediaRatio], for a card that opens the release itself. */
@Composable
fun MediaPreview(media: List<ReleaseMedia>, modifier: Modifier = Modifier) {
    val picture = media.firstOrNull { !it.isVideo } ?: return
    var failed by remember(picture.url) { mutableStateOf(false) }
    if (failed) return
    AsyncImage(
        model = picture.url,
        contentDescription = picture.name,
        modifier = modifier.fillMaxWidth().aspectRatio(MediaRatio).clip(MediaShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentScale = ContentScale.Crop,
        onError = { failed = true },
    )
}

@Composable
private fun Picture(media: ReleaseMedia) {
    var attempt by remember(media.url) { mutableIntStateOf(0) }
    var state by remember(media.url, attempt) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    var viewing by remember(media.url) { mutableStateOf(false) }
    if (state is AsyncImagePainter.State.Error) {
        MediaTile(Icons.Retry, media.name, { attempt++ }, Modifier.fillMaxWidth(), label = stringResource(Res.string.try_again))
        return
    }
    val size = (state as? AsyncImagePainter.State.Success)?.painter?.intrinsicSize
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth().animateContentSize(), contentAlignment = Alignment.Center) {
        key(attempt) {
            AsyncImage(
                model = media.url,
                contentDescription = media.name,
                modifier = (if (size == null) Modifier.fillMaxWidth().aspectRatio(MediaRatio).background(colors.surfaceContainerHighest, MediaShape) else Modifier.heightIn(max = MaxPictureHeight).aspectRatio(size.width / size.height))
                    .clip(MediaShape)
                    .clickable(enabled = size != null, role = Role.Image) { viewing = true },
                contentScale = ContentScale.Fit,
                onState = { state = it },
            )
        }
    }
    if (viewing) PictureViewer(media) { viewing = false }
}

/** [media] on the whole window: pinch or double tap to zoom, drag to move, tap or go back to leave. */
@Composable
private fun PictureViewer(media: ReleaseMedia, onDismiss: () -> Unit) {
    Window(onDismiss, DialogProperties(usePlatformDefaultWidth = false)) {
        var zoom by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var bounds by remember { mutableStateOf(IntSize.Zero) }
        // Zooms by [change] keeping the picture under [around] in place, then moves by [pan], within the picture's edges.
        fun transform(change: Float, around: Offset, pan: Offset = Offset.Zero) {
            val next = (zoom * change).coerceIn(1f, MaxZoom)
            val centre = Offset(bounds.width / 2f, bounds.height / 2f)
            val moved = offset + (around - centre - offset) * (1 - next / zoom) + pan
            val reach = centre * (next - 1)
            zoom = next
            offset = Offset(moved.x.coerceIn(-reach.x, reach.x), moved.y.coerceIn(-reach.y, reach.y))
        }
        val transformable = rememberTransformableState { centroid, change, pan, _ -> transform(change, centroid, pan) }
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                .onSizeChanged { bounds = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() },
                        onDoubleTap = { tap -> transform(if (zoom > 1f) 1f / zoom else DoubleTapZoom, tap) },
                    )
                }
                .transformable(transformable),
        ) {
            AsyncImage(
                model = media.url,
                contentDescription = media.name,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = offset.x
                    translationY = offset.y
                },
                contentScale = ContentScale.Fit,
            )
            IconButton(
                Icons.Close,
                stringResource(Res.string.close),
                onDismiss,
                Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.safeDrawing).padding(Space.sm),
                style = IconButtonStyle.Surface,
            )
        }
    }
}

/** Stands in for media that is not on screen: a video before it plays, or an image that did not load. */
@Composable
fun MediaTile(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, label: String? = null) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .aspectRatio(MediaRatio)
            .clip(MediaShape)
            .background(colors.surfaceContainerHighest)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Box(Modifier.size(Sizes.buttonLg).background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(Sizes.iconMd))
            }
            if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        }
    }
}

val MediaShape = RoundedCornerShape(Radius.md)
