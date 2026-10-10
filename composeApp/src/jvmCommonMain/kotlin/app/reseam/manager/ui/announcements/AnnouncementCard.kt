package app.reseam.manager.ui.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.reseam.manager.data.Announcement
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemText
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

@Composable
fun AnnouncementCard(announcement: Announcement, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(Radius.lg), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.padding(start = Space.xl, top = Space.md, bottom = Space.md, end = Space.sm),
            horizontalArrangement = Arrangement.spacedBy(Space.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(announcement.level.icon, contentDescription = null, tint = announcement.level.tint(), modifier = Modifier.size(Sizes.iconMd))
            ItemText(announcement.title, announcement.meta(), Modifier.weight(1f))
            IconButton(Icons.Close, stringResource(Res.string.dismiss), onDismiss)
        }
    }
}
