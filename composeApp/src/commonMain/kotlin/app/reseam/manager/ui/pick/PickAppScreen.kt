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
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.Segment
import app.reseam.manager.ui.components.SegmentedControl
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.components.StepInstruction
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme

private val ModeSegments = listOf(
    Segment(PickMode.Installed, "Installed"),
    Segment(PickMode.File, "File"),
)

@Composable
fun PickAppScreen(viewModel: PickAppViewModel, onBack: () -> Unit, onContinue: (PatchTarget) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val universal by viewModel.universalCount.collectAsStateWithLifecycle()
    val others by viewModel.others.collectAsStateWithLifecycle()
    val layout = ReseamTheme.layout
    val pickApk = rememberApkPicker(viewModel::onFilePicked)
    Screen(
        title = "New patch",
        onBack = onBack,
        header = { Stepper(current = 0) },
    ) {
        item { StepInstruction("Choose an app to patch") }
        if (viewModel.installedSupported) {
            item { SegmentedControl(ModeSegments, state.mode, viewModel::setMode) }
        }
        when (state.mode) {
            PickMode.Installed -> {
                item {
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = "Search installed apps",
                        leading = Icons.Search,
                    )
                }
                val list = candidates
                when {
                    list == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Spinner() } }
                    list.isEmpty() && universal == 0 -> item { EmptyState("No patchable apps", "None of the apps on this device match an installed patch bundle.", icon = Icons.Smartphone) }
                    list.isEmpty() -> item { EmptyState("No app-specific patches", "Your bundles have patches that work with any app. Pick one below.", icon = Icons.Smartphone) }
                    else -> appGrid(list.matching(state.query), onContinue, layout.gridColumns, layout.gutter)
                }
                if (list != null && universal > 0) {
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
                        item { SectionHeader(if (list.isEmpty()) "All apps" else "Other apps", trailing = others?.size?.toString()) }
                        when (val rest = others) {
                            null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Spinner() } }
                            else -> appGrid(rest.matching(state.query), onContinue, layout.gridColumns, layout.gutter)
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
}

private fun LazyListScope.appGrid(
    apps: List<InstalledCandidate>,
    onPick: (PatchTarget) -> Unit,
    columns: Int,
    gutter: Dp,
) {
    items(apps.chunked(columns), key = { row -> row.joinToString { it.app.packageName } }) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(gutter), modifier = Modifier.animateItem()) {
            row.forEach { candidate ->
                InstalledAppRow(
                    candidate = candidate,
                    onClick = { onPick(candidate.app.target()) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun InstalledAppRow(candidate: InstalledCandidate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        background = colors.surfaceSunken,
        borderColor = colors.divider,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(candidate.app.name, candidate.app.packageName)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(candidate.app.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(candidate.app.versionName ?: candidate.app.packageName, style = ReseamTheme.typography.monoSmall, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // The count is why a row is worth tapping, so it carries the one accent here.
            Text(
                text = if (candidate.patchCount == 1) "1 patch" else "${candidate.patchCount} patches",
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
        background = colors.surfaceSunken,
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
