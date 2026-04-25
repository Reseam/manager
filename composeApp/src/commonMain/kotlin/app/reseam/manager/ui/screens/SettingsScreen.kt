package app.reseam.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsSection
import app.reseam.manager.ui.components.RsSettingRow
import app.reseam.manager.ui.components.RsToggle
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.SettingsState
import app.reseam.manager.ui.model.ThemeMode
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun SettingsScreen(
    state: SettingsState,
    onBack: () -> Unit,
    onBundles: () -> Unit,
    onSetCheckUpdatesDaily: (Boolean) -> Unit,
    onSetAnalyticsEnabled: (Boolean) -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    versionLabel: String = "Reseam Manager",
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RsTopBar(title = "Settings", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
        ) {
            item {
                RsSection(title = "Patching") {
                    RsSettingRow(
                        title = "Bundles",
                        subtitle = "Manage patch sources",
                        icon = ReseamIcons.Puzzle,
                        onClick = onBundles,
                    )
                    RsSettingRow(
                        title = "Check for updates daily",
                        icon = ReseamIcons.Refresh,
                        showDivider = false,
                        trailing = {
                            RsToggle(
                                checked = state.checkUpdatesDaily,
                                onCheckedChange = onSetCheckUpdatesDaily,
                                size = RsButtonSize.Small,
                            )
                        },
                    )
                }
            }
            item {
                RsSection(title = "App") {
                    val themeLabel = when (state.theme) {
                        ThemeMode.System -> "System"
                        ThemeMode.Light -> "Light"
                        ThemeMode.Dark -> "Dark"
                    }
                    RsSettingRow(
                        title = "Theme",
                        subtitle = themeLabel,
                        icon = ReseamIcons.Moon,
                        onClick = {
                            val next = when (state.theme) {
                                ThemeMode.System -> ThemeMode.Light
                                ThemeMode.Light -> ThemeMode.Dark
                                ThemeMode.Dark -> ThemeMode.System
                            }
                            onSetTheme(next)
                        },
                    )
                    RsSettingRow(
                        title = "Notifications",
                        subtitle = "Update, install, build",
                        icon = ReseamIcons.Bell,
                    )
                    RsSettingRow(
                        title = "Analytics",
                        subtitle = if (state.analyticsEnabled) "On" else "Off — nothing leaves your phone",
                        icon = ReseamIcons.Info,
                        showDivider = false,
                        trailing = {
                            RsToggle(
                                checked = state.analyticsEnabled,
                                onCheckedChange = onSetAnalyticsEnabled,
                                size = RsButtonSize.Small,
                            )
                        },
                    )
                }
            }
            item {
                RsSection(title = "About") {
                    RsSettingRow(
                        title = "reseam.app",
                        subtitle = "Website",
                        icon = ReseamIcons.Globe,
                    )
                    RsSettingRow(
                        title = "Documentation",
                        icon = ReseamIcons.File,
                    )
                    RsSettingRow(
                        title = "Source code",
                        icon = ReseamIcons.GitBranch,
                        showDivider = false,
                    )
                }
            }
            item {
                Text(
                    text = versionLabel,
                    style = ReseamTheme.typography.captionSmall,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                )
            }
        }
    }
}
