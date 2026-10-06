package app.reseam.manager.ui.appdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.InfoCard
import app.reseam.manager.ui.components.InfoRow
import app.reseam.manager.ui.components.ItemTextSpacing
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme
import app.reseam.sdk.InstallMethod
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name

@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onBack: () -> Unit,
    onRepatch: (PatchTarget) -> Unit,
    onDownload: (packageName: String) -> Unit,
) {
    val app by viewModel.app.collectAsStateWithLifecycle()
    val sourceAvailable by viewModel.sourceAvailable.collectAsStateWithLifecycle()
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    val mount by viewModel.mount.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    val current = app
    Screen(
        title = current?.name ?: "App",
        onBack = onBack,
        actions = {
            if (current != null) {
                Button(onClick = viewModel::saveArtifact, variant = ButtonVariant.Ghost, size = ButtonSize.Small, icon = Icons.Download) { Text("Save") }
                Button(onClick = { viewModel.remove(onBack) }, variant = ButtonVariant.Danger, size = ButtonSize.Small, modifier = Modifier.padding(end = 8.dp)) { Text("Remove") }
            }
        },
        bottomBar = if (current == null) null else {
            {
                val mountBuild = current.installMethod == InstallMethod.MOUNT
                BottomBar { fill ->
                    when {
                        mountBuild && mount == MountStatus.Mounted -> Button(
                            onClick = viewModel::unmount,
                            modifier = fill,
                            variant = ButtonVariant.Ghost,
                            size = ButtonSize.Large,
                            enabled = !busy,
                        ) {
                            if (busy) Spinner(size = 18)
                            Text("Unmount")
                        }
                        mountBuild -> Unit
                        installed != null -> Button(onClick = viewModel::openInstalled, modifier = fill, variant = ButtonVariant.Ghost, size = ButtonSize.Large, icon = Icons.ExternalLink) { Text("Open") }
                        else -> Button(onClick = viewModel::openArtifact, modifier = fill, variant = ButtonVariant.Ghost, size = ButtonSize.Large) { Text(viewModel.artifactActionLabel) }
                    }
                    Button(
                        onClick = { viewModel.repatch(onRepatch) },
                        modifier = fill,
                        size = ButtonSize.Large,
                        enabled = !busy && if (mountBuild) mount != MountStatus.NotInstalled else sourceAvailable,
                        icon = Icons.Refresh,
                    ) { Text("Re-patch") }
                }
            }
        },
    ) {
        if (current == null) {
            item { EmptyState("Not found", "This patched app is no longer in the library.", icon = Icons.Smartphone) }
            return@Screen
        }
        item {
            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppIcon(current.name, current.packageName, size = 64.dp, iconPath = current.iconPath)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ItemTextSpacing)) {
                    Text(current.name, style = ReseamTheme.typography.title, color = colors.foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(current.versionName ?: current.packageName, style = ReseamTheme.typography.mono, color = colors.mutedForeground)
                }
            }
        }
        if (current.installMethod == InstallMethod.MOUNT) {
            item { MountBanner(current.name, mount, busy, viewModel::mount) }
        } else if (!sourceAvailable) {
            item {
                Banner(
                    message = "The APK ${current.name} was patched from was deleted, so it can't be re-patched.",
                    trailing = { Button(onClick = { onDownload(current.packageName) }, variant = ButtonVariant.Ghost, size = ButtonSize.Small) { Text("Download") } },
                )
            }
        }
        item { SectionHeader("Patches applied", trailing = current.patches.size.toString()) }
        items(current.patches, key = { "${it.bundle}/${it.id}" }) { patch ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ReseamTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Check, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ItemTextSpacing)) {
                        Text(patch.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
                        if (patch.bundle.isNotEmpty()) Text(patch.bundle, style = ReseamTheme.typography.monoSmall, color = colors.mutedForeground)
                    }
                }
            }
        }
        item { SectionHeader("Details") }
        item {
            InfoCard(
                rows = listOfNotNull(
                    if (current.installMethod == InstallMethod.MOUNT) {
                        { InfoRow("Install method", if (mount == MountStatus.Mounted) "Mounted" else "Not mounted") }
                    } else {
                        null
                    },
                    { InfoRow("Package", current.packageName, mono = true) },
                    { InfoRow("Version", current.versionName ?: "unknown") },
                    { InfoRow("Output", PlatformFile(current.apkPath).name, mono = true) },
                ),
            )
        }
    }
}

/** Why a mount build is not mounted, and the way back. */
@Composable
private fun MountBanner(name: String, mount: MountStatus, busy: Boolean, onMount: () -> Unit) {
    when (mount) {
        MountStatus.NotMounted -> Banner(
            message = "The patched $name isn't mounted.",
            trailing = {
                Button(onClick = onMount, variant = ButtonVariant.Ghost, size = ButtonSize.Small, enabled = !busy) {
                    if (busy) Spinner(size = 16)
                    Text("Mount")
                }
            },
        )
        is MountStatus.Updated -> Banner(
            "$name updated${mount.installedVersion?.let { " to $it" }.orEmpty()}, so the patched version isn't mounted. Re-patch to mount it again.",
        )
        MountStatus.NotInstalled -> Banner("$name isn't installed. Install it from the store, then patch it again.")
        MountStatus.Checking, MountStatus.Mounted -> Unit
    }
}
