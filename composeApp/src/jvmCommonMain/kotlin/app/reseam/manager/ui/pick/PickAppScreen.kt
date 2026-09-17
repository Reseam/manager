package app.reseam.manager.ui.pick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.platform.InstalledApp
import app.reseam.manager.platform.rememberApkPicker
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.CardPadding
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.IconTile
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemTextSpacing
import app.reseam.manager.ui.components.PatchFlowChrome
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.Segment
import app.reseam.manager.ui.components.SegmentedControl
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.SheetHeader
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.components.StepInstruction
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme

private val ModeSegments = listOf(
    Segment(PickMode.Apps, "Apps"),
    Segment(PickMode.File, "File"),
)

@Composable
fun PickAppScreen(viewModel: PickAppViewModel, onBack: () -> Unit, onContinue: (PatchTarget) -> Unit, onDownload: (packageName: String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var chosen by remember { mutableStateOf<InstalledApp?>(null) }
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val universal by viewModel.universalCount.collectAsStateWithLifecycle()
    val others by viewModel.others.collectAsStateWithLifecycle()
    val layout = ReseamTheme.layout
    val pickApk = rememberApkPicker(viewModel::onFilePicked)
    Screen(
        title = "New patch",
        onBack = onBack,
        header = { Stepper(current = 0) },
        chromeKey = PatchFlowChrome,
    ) {
        item { StepInstruction("Choose an app to patch") }
        item { SegmentedControl(ModeSegments, state.mode, viewModel::setMode) }
        when (state.mode) {
            PickMode.Apps -> {
                item {
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = "Search apps",
                        leading = Icons.Search,
                    )
                }
                val apps = catalog
                val empty = apps != null && apps.installed.isEmpty() && apps.saved.isEmpty() && apps.downloadable.isEmpty()
                when {
                    apps == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Spinner() } }
                    empty && universal == 0 -> item { EmptyState("No patchable apps", "Your patch bundles don't target any app yet.", icon = Icons.Smartphone) }
                    empty -> item { EmptyState("No app-specific patches", "Your bundles have patches that work with any app. Pick one below.", icon = Icons.Smartphone) }
                    else -> {
                        val sectioned = listOf(apps.installed, apps.saved, apps.downloadable).count { it.isNotEmpty() } > 1
                        if (sectioned && apps.installed.isNotEmpty()) {
                            item { SectionHeader("Installed", trailing = apps.installed.size.toString()) }
                        }
                        installedGrid(apps.installed.matching(state.query), { chosen = it }, layout.gridColumns, layout.gutter)
                        if (sectioned && apps.saved.isNotEmpty()) {
                            item { SectionHeader("Saved", trailing = apps.saved.size.toString()) }
                        }
                        grid(apps.saved.matching(state.query), key = { it.apk.id }, layout.gridColumns, layout.gutter) { candidate, modifier ->
                            val apk = candidate.apk
                            AppRow(apk.name, apk.versionName ?: apk.packageName.orEmpty(), candidate.patchCount, onClick = { onContinue(apk.target()) }, modifier) {
                                AppIcon(apk.name, apk.packageName, iconPath = apk.iconPath)
                            }
                        }
                        if (sectioned && apps.downloadable.isNotEmpty()) {
                            item { SectionHeader("Not installed", trailing = apps.downloadable.size.toString()) }
                        }
                        grid(apps.downloadable.matching(state.query), key = { it.packageName }, layout.gridColumns, layout.gutter) { candidate, modifier ->
                            AppRow(candidate.packageName, candidate.versions.first().version ?: "Any version", candidate.patchCount, onClick = { onDownload(candidate.packageName) }, modifier) {
                                IconTile(Icons.Download, tint = ReseamTheme.colors.mutedForeground)
                            }
                        }
                    }
                }
                if (apps != null && viewModel.installedSupported && universal > 0) {
                    if (!state.showingAll) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                                Button(
                                    onClick = viewModel::showAllApps,
                                    variant = ButtonVariant.Subtle,
                                    size = ButtonSize.Small,
                                ) { Text("Show all apps") }
                            }
                        }
                    } else {
                        item { SectionHeader(if (apps.installed.isEmpty()) "All apps" else "Other apps", trailing = others?.size?.toString()) }
                        when (val rest = others) {
                            null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Spinner() } }
                            else -> installedGrid(rest.matching(state.query), { chosen = it }, layout.gridColumns, layout.gutter)
                        }
                    }
                }
            }
            PickMode.File -> item {
                FilePicker(
                    selected = state.selected,
                    picking = state.pickingFile,
                    onPick = { viewModel.pickFile(pickApk) },
                    onContinue = onContinue,
                )
            }
        }
    }
    chosen?.let { app ->
        InstalledAppSheet(
            app = app,
            onDismiss = { chosen = null },
            onUse = {
                chosen = null
                onContinue(app.target())
            },
            onDownload = {
                chosen = null
                onDownload(app.packageName)
            },
        )
    }
}

private fun LazyListScope.installedGrid(apps: List<InstalledCandidate>, onPick: (InstalledApp) -> Unit, columns: Int, gutter: Dp) {
    grid(apps, key = { it.app.packageName }, columns, gutter) { candidate, modifier ->
        val app = candidate.app
        AppRow(app.name, app.versionName ?: app.packageName, candidate.patchCount, onClick = { onPick(app) }, modifier) {
            AppIcon(app.name, app.packageName)
        }
    }
}

/** An installed app can be patched as it is or replaced by another version, so tapping one asks which. */
@Composable
private fun InstalledAppSheet(app: InstalledApp, onDismiss: () -> Unit, onUse: () -> Unit, onDownload: () -> Unit) {
    Sheet(onDismiss = onDismiss) {
        SheetHeader(app.name, app.versionName?.let { "Installed version $it" } ?: app.packageName)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onUse, size = ButtonSize.Large, fullWidth = true, icon = Icons.Smartphone) { Text("Use installed version") }
            Button(onClick = onDownload, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Subtle, icon = Icons.Download) {
                Text("Download another version")
            }
        }
    }
}

private fun <T> LazyListScope.grid(
    entries: List<T>,
    key: (T) -> String,
    columns: Int,
    gutter: Dp,
    cell: @Composable (entry: T, modifier: Modifier) -> Unit,
) {
    items(entries.chunked(columns), key = { row -> row.joinToString(transform = key) }) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(gutter), modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)) {
            row.forEach { entry -> cell(entry, Modifier.weight(1f)) }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** One app to patch. The patch count is why a row is worth tapping, so it carries the one accent here. */
@Composable
private fun AppRow(
    title: String,
    detail: String,
    patchCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val colors = ReseamTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        background = colors.surface,
        borderColor = colors.divider,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            icon()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ItemTextSpacing)) {
                Text(title, style = ReseamTheme.typography.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, style = ReseamTheme.typography.monoSmall, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = if (patchCount == 1) "1 patch" else "$patchCount patches",
                style = ReseamTheme.typography.captionMedium,
                color = colors.primary,
            )
            Icon(Icons.ChevronRight, null, tint = colors.mutedForeground, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FilePicker(selected: PatchTarget?, picking: Boolean, onPick: () -> Unit, onContinue: (PatchTarget) -> Unit) {
    val colors = ReseamTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        background = colors.surface,
        borderColor = if (selected != null) colors.primaryHairline else colors.borderStrong,
        shape = ReseamTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 36.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selected != null) AppIcon(selected.name, selected.packageName, size = 72.dp, iconPath = selected.iconPath)
            else IconTile(Icons.File, size = 64.dp, background = colors.primaryFaint, tint = colors.primary)
            Text(
                text = selected?.name ?: "Choose an app file",
                style = ReseamTheme.typography.titleSmall,
                color = colors.foreground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (selected != null) listOfNotNull(selected.versionName, selected.packageName).joinToString(" · ") else "An .apk, .apkm, or .xapk file from your storage.",
                style = ReseamTheme.typography.bodySmall,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onPick,
                variant = if (selected != null) ButtonVariant.Subtle else ButtonVariant.Primary,
                size = ButtonSize.Large,
                enabled = !picking,
                icon = if (picking) null else Icons.Folder,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                if (picking) Spinner(size = 18)
                Text(if (selected != null) "Choose another" else "Browse files")
            }
            if (selected != null) {
                Button(onClick = { onContinue(selected) }, size = ButtonSize.Large, enabled = !picking) {
                    Text("Continue", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.ArrowRight, null, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
