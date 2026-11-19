package app.reseam.manager.platform

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.reseam.manager.data.ReleaseMedia
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.MediaRatio
import app.reseam.manager.ui.components.MediaShape
import app.reseam.manager.ui.components.MediaTile
import org.jetbrains.compose.resources.stringResource

private enum class Playback { Idle, Playing, Failed }

@Composable
actual fun Video(media: ReleaseMedia, modifier: Modifier) {
    var playback by remember(media.url) { mutableStateOf(Playback.Idle) }
    when (playback) {
        Playback.Idle -> MediaTile(Icons.Play, media.name, { playback = Playback.Playing }, modifier)
        Playback.Failed -> MediaTile(Icons.Retry, media.name, { playback = Playback.Playing }, modifier, label = stringResource(Res.string.try_again))
        Playback.Playing -> Player(media.url, modifier) { playback = Playback.Failed }
    }
}

@Composable
private fun Player(url: String, modifier: Modifier, onFailed: () -> Unit) {
    val context = LocalContext.current
    var ratio by remember { mutableFloatStateOf(MediaRatio) }
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) ratio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
            }

            override fun onPlayerError(error: PlaybackException) = onFailed()
        }
        player.addListener(listener)
        onDispose { player.release() }
    }
    LifecycleStartEffect(player) {
        onStopOrDispose { player.pause() }
    }
    AndroidView(
        factory = { PlayerView(it).apply { setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) } },
        modifier = modifier.aspectRatio(ratio).clip(MediaShape),
        update = { it.player = player },
    )
}
