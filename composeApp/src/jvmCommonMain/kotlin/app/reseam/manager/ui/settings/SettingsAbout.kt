package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation3.runtime.NavKey
import app.reseam.manager.EngineVersion
import app.reseam.manager.ManagerVersion
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.AboutCard
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.ExternalMark
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

private const val LicenseUrl = "https://www.gnu.org/licenses/agpl-3.0.html"
private const val NoticeUrl = "https://git.reseam.app/reseam/manager/src/branch/main/NOTICE"
private const val SourceUrl = "https://git.reseam.app/reseam/manager"
private const val MirrorUrl = "https://github.com/Reseam/manager"
private const val DonationsUrl = "https://reseam.app/donate"
private const val RequestsUrl = "https://reseam.app/requests/"

private class Social(val name: String, val url: String, val icon: ImageVector)

private val Socials = listOf(
    Social("Discord", "https://reseam.app/discord", Icons.Discord),
    Social("Reddit", "https://reseam.app/reddit", Icons.Reddit),
    Social("Telegram", "https://reseam.app/telegram", Icons.Telegram),
    Social("X", "https://reseam.app/x", Icons.X),
)

/** Manager's version, the engine's and the official patches', with links to the source. */
fun LazyListScope.aboutCard(patchesVersion: String?) {
    item {
        val uriHandler = LocalUriHandler.current
        AboutCard(
            name = stringResource(Res.string.app_name),
            version = stringResource(Res.string.version, ManagerVersion),
            meta = patchesVersion?.let { stringResource(Res.string.about_meta, EngineVersion, it) } ?: stringResource(Res.string.about_meta_engine, EngineVersion),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Text(stringResource(Res.string.about_open_source), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Button("Forgejo", { uriHandler.openUri(SourceUrl) }, style = ButtonStyle.Tonal, size = ButtonSize.Medium, icon = Icons.Forgejo)
                    Button("GitHub", { uriHandler.openUri(MirrorUrl) }, style = ButtonStyle.Tonal, size = ButtonSize.Medium, icon = Icons.GitHub)
                }
            }
        }
    }
}

/** Requests, donations, the communities, and the licenses. */
fun LazyListScope.aboutSection(open: NavKey?, onOpen: (Route) -> Unit, onReadLicense: () -> Unit) {
    pageSection(Res.string.settings_about)
    itemGroup {
        item { shape -> LinkItem(stringResource(Res.string.settings_requests), RequestsUrl, Icons.Plus, shape) }
        item { shape -> LinkItem(stringResource(Res.string.settings_donate), DonationsUrl, Icons.Heart, shape) }
        Socials.forEach { social ->
            item { shape -> LinkItem(social.name, social.url, social.icon, shape) }
        }
        item { shape ->
            GroupedItem(stringResource(Res.string.settings_license), shape, supporting = stringResource(Res.string.settings_license_supporting), icon = Icons.Scale, onClick = onReadLicense, trailing = { Chevron() })
        }
        item { shape ->
            GroupedItem(
                title = stringResource(Res.string.licenses_title),
                supporting = stringResource(Res.string.settings_licenses_supporting),
                shape = shape,
                icon = Icons.BookOpen,
                selected = open is Route.Licenses,
                onClick = { onOpen(Route.Licenses) },
                trailing = { Chevron() },
            )
        }
    }
}

@Composable
private fun LinkItem(title: String, url: String, icon: ImageVector, shape: Shape) {
    val uriHandler = LocalUriHandler.current
    GroupedItem(title, shape, supporting = url.removePrefix("https://"), icon = icon, onClick = { uriHandler.openUri(url) }, trailing = { ExternalMark() })
}

@Composable
fun LicenseSheet(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Sheet(stringResource(Res.string.settings_license), onDismiss = onDismiss) {
        Text(stringResource(Res.string.license_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(stringResource(Res.string.license_read), { uriHandler.openUri(LicenseUrl) }, Modifier.fillMaxWidth(), icon = Icons.ExternalLink)
        Button(stringResource(Res.string.license_notice), { uriHandler.openUri(NoticeUrl) }, Modifier.fillMaxWidth(), style = ButtonStyle.Tonal, icon = Icons.ExternalLink)
    }
}
