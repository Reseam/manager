package app.reseam.manager.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.Build
import app.reseam.manager.data.BuildContainer
import app.reseam.manager.platform.HumanCheck
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BannerVariant
import app.reseam.manager.ui.components.BottomBar
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.ChipVariant
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemTextSpacing
import app.reseam.manager.ui.components.PatchFlowChrome
import app.reseam.manager.ui.components.ProgressBar
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.SectionHeader
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.StepInstruction
import app.reseam.manager.ui.components.Stepper
import app.reseam.manager.ui.nav.PatchTarget
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun DownloadScreen(viewModel: DownloadViewModel, packageName: String, onBack: () -> Unit, onDownloaded: (PatchTarget) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val selectedSaved = state.build?.id in saved
    var showingAllBuilds by rememberSaveable(state.version) { mutableStateOf(false) }
    val phase = state.phase
    val name = state.appName ?: packageName
    Screen(
        title = name,
        onBack = onBack,
        header = { Stepper(current = 0) },
        chromeKey = PatchFlowChrome,
        bottomBar = {
            BottomBar { fill ->
                if (phase is DownloadPhase.Downloading || phase == DownloadPhase.Preparing) {
                    DownloadProgress(phase as? DownloadPhase.Downloading, fill)
                } else {
                    Button(
                        onClick = { viewModel.download(onDownloaded) },
                        modifier = fill,
                        size = ButtonSize.Large,
                        enabled = phase == DownloadPhase.Ready && state.build != null,
                        icon = if (selectedSaved) Icons.ArrowRight else Icons.Download,
                    ) { Text(if (selectedSaved) "Continue" else "Download") }
                }
            }
        },
    ) {
        item { StepInstruction("Choose a version") }
        when (phase) {
            DownloadPhase.Resolving -> item { Banner("Looking up $name", variant = BannerVariant.Progress) }
            is DownloadPhase.Failed -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Banner(phase.message)
                    Button(onClick = viewModel::retry, variant = ButtonVariant.Subtle, icon = Icons.Refresh) { Text("Try again") }
                }
            }
            DownloadPhase.Ready, DownloadPhase.Preparing, is DownloadPhase.Downloading -> Unit
        }
        if (state.versions.isNotEmpty()) {
            item { SectionHeader("Version") }
            items(state.versions, key = { it.version ?: "latest" }) { option ->
                OptionCard(selected = option == state.version, enabled = phase == DownloadPhase.Ready, onClick = { viewModel.chooseVersion(option) }) {
                    Text(option.version ?: "Latest", style = ReseamTheme.typography.bodyMedium, color = ReseamTheme.colors.foreground)
                    Text(versionCaption(option, state), style = ReseamTheme.typography.caption, color = ReseamTheme.colors.mutedForeground)
                }
            }
        }
        if (state.builds.isNotEmpty()) {
            val selected = state.build
            val builds = if (showingAllBuilds || selected == null) {
                state.builds.sortedWith(compareByDescending<Build> { it == state.recommended }.thenByDescending { viewModel.fits(it) })
            } else {
                listOf(selected)
            }
            item { SectionHeader("Build", trailing = state.builds.size.toString()) }
            items(builds, key = { it.id }) { build ->
                OptionCard(selected = build == selected, enabled = phase == DownloadPhase.Ready, onClick = { viewModel.chooseBuild(build) }) {
                    BuildSummary(build, recommended = build == state.recommended, fits = viewModel.fits(build), saved = build.id in saved)
                }
            }
            if (selected != null && state.builds.size > 1) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                        Button(onClick = { showingAllBuilds = !showingAllBuilds }, variant = ButtonVariant.Subtle, size = ButtonSize.Small) {
                            Text(if (showingAllBuilds) "Show fewer builds" else "Show all ${state.builds.size} builds")
                        }
                    }
                }
            }
        }
    }
    state.verification?.let { url ->
        Sheet(onDismiss = viewModel::dismissVerification) {
            HumanCheck(url, onVerified = viewModel::retry)
        }
    }
}

private fun versionCaption(option: VersionOption, state: DownloadState): String {
    val patches = if (option.patchCount == 1) "1 of ${state.patchCount} patches" else "${option.patchCount} of ${state.patchCount} patches"
    if (option.version != null) return patches
    val resolved = state.builds.firstOrNull()?.versionName?.takeIf { option == state.version } ?: "Newest stable release"
    return "$resolved · $patches"
}

@Composable
private fun OptionCard(selected: Boolean, enabled: Boolean, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = ReseamTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (selected) colors.primaryHairline else colors.divider,
        onClick = onClick,
        enabled = enabled && !selected,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ItemTextSpacing), content = content)
            if (selected) Icon(Icons.Check, contentDescription = "Selected", tint = colors.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BuildSummary(build: Build, recommended: Boolean, fits: Boolean, saved: Boolean) {
    val colors = ReseamTheme.colors
    Text(build.abis.joinToString(" + "), style = ReseamTheme.typography.bodyMedium, color = colors.foreground)
    Text("${build.minAndroid} · ${build.dpi}", style = ReseamTheme.typography.caption, color = colors.mutedForeground)
    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip(
            when (build.container) {
                BuildContainer.Apk -> "APK"
                BuildContainer.Bundle -> "Bundle"
            },
        )
        if (recommended) Chip("Best match", variant = ChipVariant.Primary)
        if (saved) Chip("Saved")
        if (!fits) Chip("Not for this device", variant = ChipVariant.Warning)
    }
}

@Composable
private fun DownloadProgress(downloading: DownloadPhase.Downloading?, modifier: Modifier) {
    val fraction = downloading?.fraction
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProgressBar(fraction ?: 0f)
        Text(
            text = when {
                fraction != null -> "Downloading ${(fraction * 100).toInt()}%"
                downloading != null -> "Starting the download"
                else -> "Getting the download link"
            },
            style = ReseamTheme.typography.caption,
            color = ReseamTheme.colors.mutedForeground,
        )
    }
}
