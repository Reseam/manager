package app.reseam.manager.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.RunLogLine
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun RsLogDrawer(
    open: Boolean,
    onToggle: () -> Unit,
    logs: List<RunLogLine>,
    onCopy: () -> Unit,
    copied: Boolean,
    modifier: Modifier = Modifier,
    currentPatch: String? = null,
) {
    val colors = ReseamTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cardElevated)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = ReseamIcons.Log,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
            Text(
                text = if (open) "Hide developer log" else "Show developer log",
                style = ReseamTheme.typography.caption,
                color = colors.mutedForeground,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (open) ReseamIcons.ChevronUp else ReseamIcons.ChevronDown,
                contentDescription = null,
                tint = colors.mutedForeground,
                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.background)
                    .border(1.dp, colors.divider, RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "LOG",
                        style = ReseamTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.mutedForeground,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (copied) colors.primaryFaint else colors.mutedElevated)
                            .clickable(onClick = onCopy)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (copied) {
                            Icon(
                                imageVector = ReseamIcons.Check,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(ReseamTheme.dimens.iconSmall),
                            )
                        }
                        Text(
                            text = if (copied) "Copied" else "Copy",
                            style = ReseamTheme.typography.captionSmall,
                            color = if (copied) colors.primary else colors.foreground,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider),
                )
                val listState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(logs) { line ->
                        val color = when {
                            line.level == "error" -> colors.logError
                            line.level == "warn" -> colors.warningForeground
                            currentPatch != null && line.patch == currentPatch -> colors.foreground
                            else -> colors.mutedForeground
                        }
                        Text(
                            text = line.message,
                            style = ReseamTheme.typography.mono10,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}
