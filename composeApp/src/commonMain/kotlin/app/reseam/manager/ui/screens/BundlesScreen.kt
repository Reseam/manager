package app.reseam.manager.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.ui.components.RsBottomSheet
import app.reseam.manager.ui.components.RsIconButton
import app.reseam.manager.ui.components.RsTopBar
import app.reseam.manager.ui.icons.ReseamIcons
import app.reseam.manager.ui.model.BundleSummary
import app.reseam.manager.ui.model.PendingBundleTrust
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun BundlesScreen(
    bundles: List<BundleSummary>,
    pending: PendingBundleTrust?,
    onBack: () -> Unit,
    onImportFromUrl: (String) -> Unit,
    onImportFromFile: () -> Unit,
    onDecideTrust: (Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onOpen: (String) -> Unit,
    onRefreshOfficial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReseamTheme.colors
    var addSheetOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            RsTopBar(title = "Bundles", onBack = onBack) {
                RsIconButton(onClick = { addSheetOpen = true }, size = 36.dp, tint = colors.primary) {
                    Icon(
                        imageVector = ReseamIcons.Plus,
                        contentDescription = "Add bundle",
                        modifier = Modifier.size(ReseamTheme.dimens.iconStandard),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
            ) {
                item { BundlesIntro() }
                items(bundles, key = { it.id }) { bundle ->
                    BundleRow(
                        bundle = bundle,
                        onClick = { onOpen(bundle.id) },
                        onRemove = { onRemove(bundle.id) },
                        onRefresh = onRefreshOfficial,
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(ReseamTheme.motion.durationBase, easing = ReseamTheme.motion.easeOut),
                            placementSpec = tween(ReseamTheme.motion.durationBase, easing = ReseamTheme.motion.easeOut),
                            fadeOutSpec = tween(ReseamTheme.motion.durationFast, easing = ReseamTheme.motion.easeOut),
                        ),
                    )
                }
                item {
                    Box(modifier = Modifier.padding(16.dp)) {
                        AddBundleButton(onClick = { addSheetOpen = true })
                    }
                }
            }
        }

        RsBottomSheet(
            visible = addSheetOpen,
            onDismissRequest = { addSheetOpen = false },
        ) {
            AddBundleSheet(
                onClose = { addSheetOpen = false },
                onSubmitUrl = { url ->
                    addSheetOpen = false
                    onImportFromUrl(url)
                },
                onSubmitFile = {
                    addSheetOpen = false
                    onImportFromFile()
                },
            )
        }

        RsBottomSheet(
            visible = pending != null,
            onDismissRequest = { onDecideTrust(false) },
        ) {
            pending?.let { TrustBundleSheet(pending = it, onDecide = onDecideTrust) }
        }
    }
}

@Composable
private fun BundlesIntro() {
    val colors = ReseamTheme.colors
    Text(
        text = "Bundles are collections of patches. The official bundle ships with Reseam; you can add more from a URL or a file.",
        style = ReseamTheme.typography.bodySmall,
        color = colors.mutedForeground,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
    )
}
