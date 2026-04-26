package app.reseam.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.reseam.manager.patcher.PatchRunStatus
import app.reseam.manager.ui.components.RsAlertBanner
import app.reseam.manager.ui.components.RsAppIcon
import app.reseam.manager.ui.components.RsBottomBar
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsButtonVariant
import app.reseam.manager.ui.components.RsCard
import app.reseam.manager.ui.components.RsChip
import app.reseam.manager.ui.components.RsChipVariant
import app.reseam.manager.ui.components.RsLogDrawer
import app.reseam.manager.ui.components.PatchFlowSteps
import app.reseam.manager.ui.components.RsStepper
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.PatchEditorItem
import app.reseam.manager.ui.model.PatchRunState
import app.reseam.manager.ui.model.RunStatus
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RunScreen(
    state: PatchRunState,
    appName: String?,
    appPackage: String?,
    patches: List<PatchEditorItem>,
    onCopyLogs: () -> Unit,
    copied: Boolean,
    onInstall: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    val finished = state.status == RunStatus.Finished || state.status == RunStatus.Failed
    val title = when {
        finished && state.hasFailure -> "Ready (with warnings)"
        finished -> "Ready"
        else -> "Patching"
    }
    var logOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RsTopBar(title = title)
        RsStepper(current = 2, steps = PatchFlowSteps)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp),
        ) {
            if (!finished) {
                item {
                    RunningView(
                        state = state,
                        appName = appName,
                        appPackage = appPackage,
                        patches = patches,
                        logOpen = logOpen,
                        onToggleLog = { logOpen = !logOpen },
                        onCopyLogs = onCopyLogs,
                        copied = copied,
                    )
                }
            } else {
                item {
                    DoneView(
                        state = state,
                        appName = appName,
                        appPackage = appPackage,
                        patches = patches,
                        logOpen = logOpen,
                        onToggleLog = { logOpen = !logOpen },
                        onCopyLogs = onCopyLogs,
                        copied = copied,
                    )
                }
            }
        }
        if (finished) {
            RsBottomBar {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RsButton(
                        onClick = onInstall,
                        size = RsButtonSize.Large,
                        fullWidth = true,
                    ) {
                        Icon(
                            imageVector = ReseamIcons.Download,
                            contentDescription = null,
                            modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                        )
                        Text("Install patched app")
                    }
                    RsButton(
                        onClick = onDone,
                        size = RsButtonSize.Large,
                        fullWidth = true,
                        variant = RsButtonVariant.Ghost,
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningView(
    state: PatchRunState,
    appName: String?,
    appPackage: String?,
    patches: List<PatchEditorItem>,
    logOpen: Boolean,
    onToggleLog: () -> Unit,
    onCopyLogs: () -> Unit,
    copied: Boolean,
) {
    val colors = ReseamTheme.colors
    val activeName = state.currentPatch
    val activeIndex = patches.indexOfFirst { it.metadata.name == activeName }.coerceAtLeast(0)
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
        ) {
            RsAppIcon(name = appName, packageName = appPackage, size = 52.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "PATCHING",
                    style = ReseamTheme.typography.label.copy(letterSpacing = 0.18.em, fontWeight = FontWeight.Bold),
                    color = colors.primary,
                )
                Text(
                    text = appName ?: "—",
                    style = ReseamTheme.typography.title,
                    color = colors.foreground,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.progressPercent.toString(),
                        style = ReseamTheme.typography.title.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.foreground,
                    )
                    Text(
                        text = "%",
                        style = ReseamTheme.typography.caption,
                        color = colors.mutedForeground,
                        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                    )
                }
                if (patches.isNotEmpty()) {
                    Text(
                        text = "${activeIndex + 1} / ${patches.size}",
                        style = ReseamTheme.typography.captionSmall,
                        color = colors.mutedForeground,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(colors.muted),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(state.progressPercent.coerceIn(0, 100) / 100f)
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(listOf(colors.primary, Color(0xFFC9F4DB))),
                    ),
            )
        }
        Box(modifier = Modifier.height(18.dp))
        if (activeName != null) {
            RsCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = colors.primaryHairline,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "NOW APPLYING",
                            style = ReseamTheme.typography.label.copy(letterSpacing = 0.18.em, fontWeight = FontWeight.Bold),
                            color = colors.mutedForeground,
                        )
                        Text(
                            text = activeName,
                            style = ReseamTheme.typography.titleSmall,
                            color = colors.foreground,
                        )
                    }
                    Icon(
                        imageVector = ReseamIcons.Sparkles,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                    )
                }
            }
            Box(modifier = Modifier.height(16.dp))
        }
        Text(
            text = "QUEUE",
            style = ReseamTheme.typography.label.copy(letterSpacing = 0.2.em, fontWeight = FontWeight.Bold),
            color = colors.mutedForeground,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp),
        )
        patches.forEachIndexed { i, p ->
            QueueRow(
                name = p.metadata.name,
                status = state.patchStatuses[p.metadata.name],
                active = i == activeIndex && state.patchStatuses[p.metadata.name] == null,
            )
        }
        Box(modifier = Modifier.height(14.dp))
        RsLogDrawer(
            open = logOpen,
            onToggle = onToggleLog,
            logs = state.logs,
            onCopy = onCopyLogs,
            copied = copied,
            currentPatch = activeName,
        )
    }
}

@Composable
private fun DoneView(
    state: PatchRunState,
    appName: String?,
    appPackage: String?,
    patches: List<PatchEditorItem>,
    logOpen: Boolean,
    onToggleLog: () -> Unit,
    onCopyLogs: () -> Unit,
    copied: Boolean,
) {
    val colors = ReseamTheme.colors
    val hasFailure = state.hasFailure
    val applied = patches.count { state.patchStatuses[it.metadata.name] == PatchRunStatus.Applied }
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (hasFailure) colors.warningSoft else colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (hasFailure) ReseamIcons.TriangleAlert else ReseamIcons.Check,
                contentDescription = null,
                tint = if (hasFailure) colors.warningForeground else Color.Black,
                modifier = Modifier.size(if (hasFailure) 32.dp else 34.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${appName ?: "App"} is patched",
                style = ReseamTheme.typography.headline,
                color = colors.foreground,
            )
            Text(
                text = "$applied of ${patches.size} patches applied" +
                    if (hasFailure) ", ${patches.size - applied} failed" else "",
                style = ReseamTheme.typography.bodySmall,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .widthIn(max = 280.dp),
            )
        }
        if (state.artifact != null) {
            RsCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = colors.borderStrong,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RsAppIcon(name = appName, packageName = appPackage, size = 44.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = state.artifact.path.substringAfterLast('/'),
                            style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                            color = colors.foreground,
                        )
                        Text(
                            text = "signed · ${state.artifact.kind.name.lowercase()}",
                            style = ReseamTheme.typography.captionSmall.copy(fontFamily = ReseamTheme.typography.mono),
                            color = colors.mutedForeground,
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "PATCHES",
                style = ReseamTheme.typography.label.copy(letterSpacing = 0.2.em, fontWeight = FontWeight.Bold),
                color = colors.mutedForeground,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            patches.forEach { p ->
                QueueRow(
                    name = p.metadata.name,
                    status = state.patchStatuses[p.metadata.name],
                    active = false,
                    boxed = true,
                )
            }
        }
        if (hasFailure) {
            val failedNames = patches.filter { state.patchStatuses[it.metadata.name] == PatchRunStatus.Failed }
                .joinToString(", ") { it.metadata.name }
            RsAlertBanner(
                message = "$failedNames failed to apply. Copy logs and share with the patch author.",
                horizontalPadding = 12.dp,
                verticalPadding = 10.dp,
            ) {
                Icon(
                    imageVector = ReseamIcons.TriangleAlert,
                    contentDescription = null,
                    tint = colors.warningForeground,
                    modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                )
            }
        }
        RsLogDrawer(
            open = logOpen,
            onToggle = onToggleLog,
            logs = state.logs,
            onCopy = onCopyLogs,
            copied = copied,
        )
    }
}

@Composable
private fun QueueRow(
    name: String,
    status: PatchRunStatus?,
    active: Boolean,
    boxed: Boolean = false,
) {
    val colors = ReseamTheme.colors
    val rowColor = when {
        boxed -> Color(0xFF0F0F0F)
        active -> colors.cardElevated
        else -> Color.Transparent
    }
    RsCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (boxed) 2.dp else 0.dp),
        background = rowColor,
        borderColor = if (boxed) colors.divider else null,
        cornerRadius = 10.dp,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusDot(status = status, active = active)
            Text(
                text = name,
                style = ReseamTheme.typography.bodySmall,
                color = if (status != null || active) colors.foreground else colors.mutedForeground,
                modifier = Modifier.weight(1f),
            )
            if (status == PatchRunStatus.Failed) {
                RsChip(text = "Failed", variant = RsChipVariant.Amber)
            } else if (status == PatchRunStatus.Skipped) {
                RsChip(text = "Skipped", variant = RsChipVariant.Default)
            }
        }
    }
}

@Composable
private fun StatusDot(status: PatchRunStatus?, active: Boolean) {
    val colors = ReseamTheme.colors
    when {
        status == PatchRunStatus.Applied -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ReseamIcons.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(14.dp),
            )
        }
        status == PatchRunStatus.Failed -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.warningSoft)
                .border(1.dp, colors.warningHairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ReseamIcons.TriangleAlert,
                contentDescription = null,
                tint = colors.warningForeground,
                modifier = Modifier.size(14.dp),
            )
        }
        status == PatchRunStatus.Skipped -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.muted)
                .border(1.dp, colors.divider, CircleShape),
        )
        active -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.primarySoft)
                .border(1.dp, colors.primaryHairline, CircleShape),
        )
        else -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(colors.mutedElevated),
        )
    }
}
