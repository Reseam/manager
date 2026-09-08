package app.reseam.manager.ui.pick

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.IconTile
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.Segment
import app.reseam.manager.ui.components.SegmentedControl
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.components.StepIntro
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.platform.rememberApkPicker
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme

private val ModeSegments = listOf(
    Segment(PickMode.Installed, "Installed", Icons.Smartphone),
    Segment(PickMode.File, "File", Icons.Folder),
)

@Composable
fun PickAppScreen(viewModel: PickAppViewModel, onBack: () -> Unit, onContinue: (PatchTarget) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val colors = ReseamTheme.colors
    val pickApk = rememberApkPicker(viewModel::onFilePicked)
    Screen(
        title = "New patch",
        onBack = onBack,
        header = { Stepper(current = 0) },
        bottomBar = {
            BottomBar {
                val selected = state.selected
                Button(onClick = { selected?.let(onContinue) }, size = ButtonSize.Large, fullWidth = true, enabled = selected != null && !state.pickingFile) {
                    Text(if (selected != null) "Continue with ${selected.name}" else "Pick an app", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (selected != null) Icon(Icons.ArrowRight, null, modifier = Modifier.size(18.dp))
                }
            }
        },
    ) {
        item { StepIntro(step = 1, title = "Pick an app", body = "Choose what to patch. The original app stays untouched.") }
        if (viewModel.installedSupported) {
            item {
                SegmentedControl(ModeSegments, state.mode, viewModel::setMode, Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
            }
        }
        when (state.mode) {
            PickMode.Installed -> {
                item {
                    TextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        placeholder = "Search installed apps",
                        leading = Icons.Search,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                    )
                }
                val list = candidates
                when {
                    list == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Spinner() } }
                    list.isEmpty() -> item { EmptyState("No patchable apps", "None of the apps on this device match an installed patch bundle.") }
                    else -> items(list.matching(state.query), key = { it.app.packageName }) { candidate ->
                        InstalledAppRow(
                            candidate = candidate,
                            selected = state.selected?.packageName == candidate.app.packageName,
                            onClick = { viewModel.select(candidate.app) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            PickMode.File -> item {
                FilePicker(
                    selected = state.selected,
                    picking = state.pickingFile,
                    onPick = { viewModel.pickFile(pickApk) },
                )
            }
        }
    }
}

@Composable
private fun InstalledAppRow(candidate: InstalledCandidate, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        background = if (selected) colors.primaryFaint else colors.background,
        borderColor = if (selected) colors.primaryHairline else colors.background,
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AppIcon(candidate.app.name, candidate.app.packageName)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(candidate.app.name, style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
                Text(candidate.app.versionName ?: candidate.app.packageName, style = ReseamTheme.typography.monoSmall, color = colors.mutedForeground)
            }
            Chip(
                text = if (candidate.patchCount == 1) "1 patch" else "${candidate.patchCount} patches",
                variant = if (selected) ChipVariant.SolidPrimary else ChipVariant.Primary,
            )
        }
    }
}

@Composable
private fun FilePicker(selected: PatchTarget?, picking: Boolean, onPick: () -> Unit) {
    val colors = ReseamTheme.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            background = colors.surfaceSunken,
            borderColor = if (selected != null) colors.primaryHairline else colors.borderStrong,
            shape = ReseamTheme.shapes.large,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (selected != null) AppIcon(selected.name, selected.packageName, size = 56.dp, iconPath = selected.iconPath)
                else IconTile(Icons.File, size = 48.dp, background = colors.primaryFaint, tint = colors.primary)
                Text(selected?.name ?: "Choose an APK bundle", style = ReseamTheme.typography.bodyMedium, color = colors.foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (selected != null) listOfNotNull(selected.versionName, selected.packageName).joinToString(" · ") else "An .apk, .apkm, or .xapk file from your storage.",
                    style = ReseamTheme.typography.caption,
                    color = colors.mutedForeground,
                )
                Button(onClick = onPick, variant = ButtonVariant.Subtle, enabled = !picking, modifier = Modifier.padding(top = 6.dp)) {
                    if (picking) Spinner(size = 16) else Icon(Icons.Folder, null, modifier = Modifier.size(18.dp))
                    Text(if (selected != null) "Choose another" else "Browse files")
                }
            }
        }
    }
}
