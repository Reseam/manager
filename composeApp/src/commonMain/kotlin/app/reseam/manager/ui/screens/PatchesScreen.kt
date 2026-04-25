package app.reseam.manager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.reseam.manager.patcher.InputOptionValue
import app.reseam.manager.patcher.OptionKind
import app.reseam.manager.ui.components.RsButton
import app.reseam.manager.ui.components.RsButtonSize
import app.reseam.manager.ui.components.RsButtonVariant
import app.reseam.manager.ui.components.RsBottomBar
import app.reseam.manager.ui.components.RsChip
import app.reseam.manager.ui.components.RsChipVariant
import app.reseam.manager.ui.components.RsStepLabel
import app.reseam.manager.ui.components.RsStepper
import app.reseam.manager.ui.components.RsToggle
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.PatchEditorItem
import app.reseam.manager.ui.model.PatchEditorState
import app.reseam.manager.ui.model.PatchOptionEditorValue
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
    val colors = ReseamTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        RsTopBar(title = appName ?: "Patches", onBack = onBack) {
            RsChip(text = "${state.activeCount} on", variant = RsChipVariant.Primary)
        }
        RsStepper(current = 1, steps = listOf("Inputs", "Patches", "Run"))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                    RsStepLabel(step = 2)
                    Text(
                        text = "What to change",
                        style = ReseamTheme.typography.display,
                        color = colors.foreground,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    )
                    Text(
                        text = "Toggle features on or off. Tap a row to tune it.",
                        style = ReseamTheme.typography.bodySmall,
                        color = colors.mutedForeground,
                    )
                }
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
    }
}

@Composable
private fun PillAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReseamTheme.colors
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.cardElevated)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
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
    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (on) colors.cardElevated else Color(0xFF0E0E0E))
            .border(
                1.dp,
                if (on) colors.primaryHairline else colors.divider,
                RoundedCornerShape(14.dp),
            )
            .clickable(enabled = canExpand, onClick = onExpandToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
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
                    OptionControl(
                        meta = meta,
                        value = current,
                        onChange = { onUpdateOption(meta.key, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionControl(
    meta: app.reseam.manager.patcher.OptionMetadata,
    value: PatchOptionEditorValue?,
    onChange: (InputOptionValue) -> Unit,
) {
    val colors = ReseamTheme.colors
    val current = value?.value ?: meta.defaultValue
    val validValues = value?.validValues ?: meta.validValues ?: emptyList()
    val isSelect = meta.optionType == OptionKind.String && validValues.isNotEmpty()
    val labelRow: @Composable () -> Unit = {
        Text(
            text = meta.title,
            style = ReseamTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = colors.foreground,
        )
    }
    when {
        meta.optionType == OptionKind.Bool -> {
            val boolVal = (current as? InputOptionValue.BoolValue)?.value ?: false
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RsToggle(
                    checked = boolVal,
                    onCheckedChange = { onChange(InputOptionValue.BoolValue(it)) },
                    size = RsButtonSize.Small,
                )
                Text(
                    text = meta.title,
                    style = ReseamTheme.typography.caption,
                    color = colors.foreground,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        isSelect -> {
            val str = (current as? InputOptionValue.StringValue)?.value ?: ""
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                labelRow()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.background)
                        .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    validValues.forEach { v ->
                        SegmentChoice(
                            label = v,
                            selected = str == v,
                            onClick = { onChange(InputOptionValue.StringValue(v)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        meta.optionType == OptionKind.Path -> {
            val str = (current as? InputOptionValue.PathValue)?.value ?: ""
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                labelRow()
                OptionInputBox {
                    Icon(
                        imageVector = ReseamIcons.Folder,
                        contentDescription = null,
                        tint = colors.mutedForeground,
                        modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                    )
                    OptionTextField(
                        value = str,
                        onValueChange = { onChange(InputOptionValue.PathValue(it)) },
                        mono = true,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.mutedElevated)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Browse",
                            style = ReseamTheme.typography.captionSmall.copy(fontWeight = FontWeight.Medium),
                            color = colors.foreground,
                        )
                    }
                }
            }
        }
        meta.optionType == OptionKind.Int || meta.optionType == OptionKind.Float -> {
            val text = when (current) {
                is InputOptionValue.IntValue -> current.value.toString()
                is InputOptionValue.FloatValue -> current.value.toString()
                else -> ""
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                labelRow()
                OptionInputBox {
                    OptionTextField(
                        value = text,
                        onValueChange = { input ->
                            if (meta.optionType == OptionKind.Int) {
                                input.toLongOrNull()?.let { onChange(InputOptionValue.IntValue(it)) }
                            } else {
                                input.toDoubleOrNull()?.let { onChange(InputOptionValue.FloatValue(it)) }
                            }
                        },
                        mono = true,
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        else -> {
            val str = when (current) {
                is InputOptionValue.StringValue -> current.value
                is InputOptionValue.PathValue -> current.value
                else -> ""
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                labelRow()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.background)
                        .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    OptionTextField(
                        value = str,
                        onValueChange = { onChange(InputOptionValue.StringValue(it)) },
                        mono = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionInputBox(content: @Composable RowScope.() -> Unit) {
    val colors = ReseamTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.background)
            .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun OptionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    mono: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = ReseamTheme.colors
    val style = ReseamTheme.typography.caption.copy(
        color = colors.foreground,
        fontFamily = if (mono) ReseamTheme.typography.mono else ReseamTheme.typography.sans,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier,
    )
}

@Composable
private fun SegmentChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.accent else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ReseamTheme.typography.captionSmall.copy(fontWeight = FontWeight.Medium),
            color = if (selected) colors.foreground else colors.mutedForeground,
        )
    }
}
