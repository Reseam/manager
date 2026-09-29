package app.reseam.manager.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.ManagerVersion
import app.reseam.manager.data.ManagerUpdate
import app.reseam.manager.data.ManagerUpdatePhase
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ProgressBar
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.saved.byteSize
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun UpdateCard(
    update: ManagerUpdate,
    installable: Boolean,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onOpenRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val phase = update.phase
    val failed = phase is ManagerUpdatePhase.Failed
    Card(
        modifier = modifier.fillMaxWidth(),
        background = if (failed) colors.warningSoft else colors.primaryFaint,
        borderColor = if (failed) colors.warningHairline else colors.primaryHairline,
        shape = ReseamTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                when (phase) {
                    ManagerUpdatePhase.Installing -> Spinner(size = 20)
                    is ManagerUpdatePhase.Failed -> Icon(Icons.TriangleAlert, null, tint = colors.warningForeground, modifier = Modifier.size(20.dp))
                    else -> Icon(Icons.Refresh, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = when (phase) {
                            ManagerUpdatePhase.Available -> "Reseam Manager ${update.version} is available"
                            is ManagerUpdatePhase.Downloading, ManagerUpdatePhase.Installing -> "Updating Reseam Manager"
                            is ManagerUpdatePhase.Failed -> "Reseam Manager couldn't update"
                        },
                        style = ReseamTheme.typography.captionMedium,
                        color = colors.foreground,
                    )
                    Text(
                        text = when (phase) {
                            ManagerUpdatePhase.Available -> if (installable) "You're on version $ManagerVersion" else "Get it from the release page"
                            is ManagerUpdatePhase.Downloading -> "Downloading" + phase.total?.let { " · ${byteSize(phase.written)} of ${byteSize(it)}" }.orEmpty()
                            ManagerUpdatePhase.Installing -> "Installing. The app closes to finish."
                            is ManagerUpdatePhase.Failed -> phase.message
                        },
                        style = ReseamTheme.typography.captionSmall,
                        color = if (failed) colors.warningForeground else colors.mutedForeground,
                    )
                }
                when (phase) {
                    ManagerUpdatePhase.Available ->
                        if (installable) {
                            Button(onClick = onInstall, size = ButtonSize.Small) { Text("Update") }
                        } else {
                            Button(onClick = onOpenRelease, size = ButtonSize.Small) { Text("Download") }
                        }
                    is ManagerUpdatePhase.Downloading -> Button(onClick = onCancel, size = ButtonSize.Small, variant = ButtonVariant.Ghost) { Text("Cancel") }
                    ManagerUpdatePhase.Installing -> Unit
                    is ManagerUpdatePhase.Failed -> Button(onClick = onInstall, size = ButtonSize.Small) { Text("Try again") }
                }
            }
            AnimatedVisibility(phase is ManagerUpdatePhase.Downloading) {
                val downloading = phase as? ManagerUpdatePhase.Downloading
                ProgressBar(downloading?.total?.let { downloading.written.toFloat() / it } ?: 0f)
            }
        }
    }
}
