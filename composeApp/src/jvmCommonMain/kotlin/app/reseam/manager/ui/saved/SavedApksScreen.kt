package app.reseam.manager.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.SavedApkOrigin
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.IconButton
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemTextSpacing
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun SavedApksScreen(viewModel: SavedApksViewModel, onBack: (() -> Unit)?) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    Screen(title = "Saved APKs", onBack = onBack) {
        item {
            Text(
                text = "Apps you downloaded or picked from a file. They stay here so you can patch again without fetching them, until you delete them.",
                style = ReseamTheme.typography.bodySmall,
                color = colors.mutedForeground,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        val list = rows
        when {
            list == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Spinner() } }
            list.isEmpty() -> item { EmptyState("Nothing saved", "APKs you download for patching show up here.", icon = Icons.Download) }
            else -> {
                item { SectionHeader(if (list.size == 1) "1 APK" else "${list.size} APKs", trailing = byteSize(list.sumOf { it.apk.sizeBytes })) }
                items(list, key = { it.apk.id }) { row -> SavedApkCard(row, onDelete = { viewModel.remove(row.apk) }) }
            }
        }
    }
}

@Composable
private fun SavedApkCard(row: SavedApkRow, onDelete: () -> Unit) {
    val colors = ReseamTheme.colors
    val apk = row.apk
    Card(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(apk.name, apk.packageName, iconPath = apk.iconPath)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(ItemTextSpacing)) {
                    Text(apk.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = listOfNotNull(apk.versionName, byteSize(apk.sizeBytes)).joinToString(" · "),
                        style = ReseamTheme.typography.monoSmall,
                        color = colors.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip(
                        when (apk.origin) {
                            SavedApkOrigin.Download -> "Downloaded"
                            SavedApkOrigin.File -> "From a file"
                        },
                    )
                    if (row.inUse) Chip("Used to re-patch", variant = ChipVariant.Primary)
                }
            }
            IconButton(Icons.Trash, "Delete ${apk.name}", onClick = onDelete)
        }
    }
}
