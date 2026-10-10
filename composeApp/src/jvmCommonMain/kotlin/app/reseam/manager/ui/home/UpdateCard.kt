package app.reseam.manager.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import app.reseam.manager.ManagerVersion
import app.reseam.manager.data.Bundle
import app.reseam.manager.data.BundleOffer
import app.reseam.manager.data.BundleUpdate
import app.reseam.manager.data.ManagerUpdate
import app.reseam.manager.data.ManagerUpdatePhase
import app.reseam.manager.data.WhatsNew
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemText
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.downloadProgress
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateCard(update: ManagerUpdate, installable: Boolean, onInstall: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val phase = update.phase
    val (title, supporting) = when (phase) {
        ManagerUpdatePhase.Available -> stringResource(Res.string.update_available, update.version) to stringResource(Res.string.update_current, ManagerVersion)
        is ManagerUpdatePhase.Downloading -> stringResource(Res.string.update_available, update.version) to
            downloadProgress(phase.written, phase.total)
        ManagerUpdatePhase.Installing -> stringResource(Res.string.update_available, update.version) to stringResource(Res.string.update_installing)
        is ManagerUpdatePhase.Failed -> stringResource(Res.string.update_failed) to phase.error.describe()
    }
    StatusCard(
        title = title,
        supporting = supporting,
        modifier = modifier,
        below = {
            if (phase is ManagerUpdatePhase.Downloading && phase.total != null) {
                LinearProgressIndicator(progress = { phase.written.toFloat() / phase.total }, modifier = Modifier.fillMaxWidth(), strokeCap = StrokeCap.Round)
            }
        },
    ) {
        when (phase) {
            ManagerUpdatePhase.Available -> Button(
                label = stringResource(if (installable) Res.string.update else Res.string.update_download),
                onClick = { if (installable) onInstall() else uriHandler.openUri(update.releaseUrl) },
                size = ButtonSize.Medium,
            )
            is ManagerUpdatePhase.Downloading -> Button(stringResource(Res.string.cancel), onCancel, style = ButtonStyle.Text)
            ManagerUpdatePhase.Installing -> Unit
            is ManagerUpdatePhase.Failed -> Button(stringResource(Res.string.try_again), onInstall, size = ButtonSize.Medium)
        }
    }
}

@Composable
fun BundleUpdateCard(update: BundleUpdate, modifier: Modifier = Modifier) {
    StatusCard(stringResource(Res.string.bundle_updating, update.name, update.version), stringResource(Res.string.bundle_updating_supporting), modifier)
}

@Composable
fun BundleOfferCard(offer: BundleOffer, onUpdate: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    StatusCard(
        title = stringResource(Res.string.bundle_available, offer.name, offer.release.version),
        supporting = stringResource(Res.string.bundle_whats_new),
        modifier = modifier,
        onClick = onOpen,
    ) {
        Button(stringResource(Res.string.update), onUpdate, size = ButtonSize.Medium)
    }
}

@Composable
fun WhatsNewCard(bundle: Bundle, whatsNew: WhatsNew, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    StatusCard(
        title = stringResource(Res.string.bundle_updated, bundle.name, whatsNew.version),
        supporting = if (whatsNew.added.isEmpty()) stringResource(Res.string.bundle_whats_new) else pluralStringResource(Res.plurals.bundle_new_patches, whatsNew.added.size, whatsNew.added.size),
        modifier = modifier,
        onClick = onOpen,
    ) {
        IconButton(Icons.Close, stringResource(Res.string.dismiss), onDismiss)
    }
}

@Composable
private fun StatusCard(
    title: String,
    supporting: String,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
    below: @Composable () -> Unit = {},
    action: @Composable () -> Unit = {},
) {
    val shape = RoundedCornerShape(Radius.lg)
    Surface(
        modifier = modifier.fillMaxWidth().clip(shape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(Space.xl), verticalArrangement = Arrangement.spacedBy(Space.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
                ItemText(title, supporting, Modifier.weight(1f))
                action()
            }
            below()
        }
    }
}
