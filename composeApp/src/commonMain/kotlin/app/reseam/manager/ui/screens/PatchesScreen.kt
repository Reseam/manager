package app.reseam.manager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.ui.components.PatchFlowIntro
import app.reseam.manager.ui.components.PatchFlowScaffold
import app.reseam.manager.ui.components.PatchOptionControl
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsBottomBar
import app.reseam.manager.ui.components.RsCard
import app.reseam.manager.ui.components.RsChip
import app.reseam.manager.ui.components.RsChipVariant
import app.reseam.manager.ui.components.RsToggle
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.PatchEditorItem
import app.reseam.manager.ui.model.PatchEditorState
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun PatchesScreen(
    state: PatchEditorState,
    appName: String?,
    onBack: () -> Unit,
    onTogglePatch: (name: String, enabled: Boolean) -> Unit,
    onOpenOptions: (patchName: String?) -> Unit,
    onUpdateOption: (patchName: String, key: String, value: InputOptionValue) -> Unit,
    onSelectAll: () -> Unit,
    onResetDefaults: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PatchFlowScaffold(
        title = appName ?: "Patches",
        currentStep = 1,
        modifier = modifier,
        onBack = onBack,
        actions = {
            RsChip(text = "${state.activeCount} on", variant = RsChipVariant.Primary)
        },
        bottomBar = {
            RsBottomBar {
                RsButton(
                    onClick = onContinue,
                    size = RsButtonSize.Large,
                    fullWidth = true,
                    enabled = state.canPatch,
                ) {
                    Text("Patch app")
                    Icon(
                        imageVector = ReseamIcons.Sparkles,
                        contentDescription = null,
                        modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                    )
                }
            }
        },
    ) {
            item {
                PatchFlowIntro(
                    step = 2,
                    title = "What to change",
                    body = "Toggle features on or off. Tap a row to tune it.",
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PillAction(text = "Select all", onClick = onSelectAll, modifier = Modifier.weight(1f))
                    PillAction(text = "Defaults", onClick = onResetDefaults, modifier = Modifier.weight(1f))
                }
            }
            items(state.patches, key = { it.metadata.name }) { item ->
                PatchRow(
                    item = item,
                    isOpen = state.openPatchName == item.metadata.name,
                    onToggle = { onTogglePatch(item.metadata.name, it) },
                    onExpandToggle = {
                        if (state.openPatchName == item.metadata.name) onOpenOptions(null)
                        else onOpenOptions(item.metadata.name)
                    },
                    onUpdateOption = { key, value -> onUpdateOption(item.metadata.name, key, value) },
                )
            }
    }
}

@Composable
private fun PillAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    RsCard(
        modifier = modifier.height(32.dp),
        background = colors.cardElevated,
        borderColor = colors.border,
        cornerRadius = 10.dp,
        onClick = onClick,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ReseamTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = colors.foreground,
        )
    }
}

@Composable
private fun PatchRow(
    item: PatchEditorItem,
    isOpen: Boolean,
    onToggle: (Boolean) -> Unit,
    onExpandToggle: () -> Unit,
    onUpdateOption: (String, InputOptionValue) -> Unit,
) {
    val colors = ReseamTheme.colors
    val on = item.enabled
    val optionCount = item.optionCount
    val canExpand = optionCount > 0
    RsCard(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth(),
        background = if (on) colors.cardElevated else colors.surfaceSunken,
        borderColor = if (on) colors.primaryHairline else colors.divider,
        onClick = onExpandToggle,
        enabled = canExpand,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = item.metadata.name,
                            style = ReseamTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                            color = colors.foreground,
                        )
                        if (item.required) {
                            RsChip(text = "Required", variant = RsChipVariant.Default)
                        }
                    }
                    Text(
                        text = item.metadata.description,
                        style = ReseamTheme.typography.caption,
                        color = colors.mutedForeground,
                    )
                    if (canExpand && on) {
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = if (optionCount == 1) "1 option" else "$optionCount options",
                                style = ReseamTheme.typography.captionSmall.copy(fontWeight = FontWeight.Medium),
                                color = colors.primary,
                            )
                            Icon(
                                imageVector = if (isOpen) ReseamIcons.ChevronUp else ReseamIcons.ChevronDown,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                            )
                        }
                    }
                }
                RsToggle(
                    checked = on,
                    onCheckedChange = onToggle,
                    enabled = !item.required,
                )
            }
            AnimatedVisibility(
                visible = isOpen && on && canExpand,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.borderStrong),
                    )
                    Spacer(Modifier.height(0.dp))
                    item.metadata.options.forEach { meta ->
                        val current = item.options[meta.key]
                        PatchOptionControl(
                            meta = meta,
                            value = current,
                            onChange = { onUpdateOption(meta.key, it) },
                        )
                    }
                }
            }
        }
    }
}
