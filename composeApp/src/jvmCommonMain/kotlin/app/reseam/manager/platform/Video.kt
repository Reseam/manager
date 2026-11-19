package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.reseam.manager.data.ReleaseMedia

/** A release's video: played in place where the platform has a player, handed to the system's otherwise. */
@Composable
expect fun Video(media: ReleaseMedia, modifier: Modifier = Modifier)
