package app.reseam.manager.ui.pick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Card
import app.reseam.manager.ui.components.EmptyState
import app.reseam.manager.ui.components.IconTile
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemTextSpacing
import app.reseam.manager.ui.components.PatchFlowChrome
import app.reseam.manager.ui.components.Screen
import app.reseam.manager.ui.components.ScreenFrame
import app.reseam.manager.ui.components.SectionSpacing
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
    val sheetWork by viewModel.sheetWork.collectAsStateWithLifecycle()
    val layout = ReseamTheme.layout
    val pickApk = rememberApkPicker(viewModel::onFilePicked)
    if (layout.twoPane) {
        DesktopPicker(
            viewModel = viewModel,
            state = state,
            catalog = catalog,
            universal = universal,
            others = others,
            onBack = onBack,
            onContinue = onContinue,
            onDownload = onDownload,
            onInstalled = { chosen = it },
            pickApk = pickApk,
        )
    } else Screen(
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
                        savedGrid(apps.saved.matching(state.query), onContinue, layout.gridColumns, layout.gutter)
                        if (sectioned && apps.downloadable.isNotEmpty()) {
                            item { SectionHeader("Not installed", trailing = apps.downloadable.size.toString()) }
                        }
                        downloadableGrid(apps.downloadable.matching(state.query), onDownload, layout.gridColumns, layout.gutter)
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
            mountAvailable = viewModel.mountAvailable,
            work = sheetWork,
            onDismiss = {
                chosen = null
                viewModel.resetSheet()
            },
            onUse = {
                viewModel.use(app) { target ->
                    chosen = null
                    onContinue(target)
                }
            },
            onMount = {
                viewModel.mount(app) { target ->
                    chosen = null
                    onContinue(target)
                }
            },
            onDownload = {
                chosen = null
                onDownload(app.packageName)
            },
        )
    }
}

/** Local apps stay in a library column while the downloadable catalog uses the remaining space. */
@Composable
private fun DesktopPicker(
    viewModel: PickAppViewModel,
    state: PickAppState,
    catalog: PickCatalog?,
    universal: Int,
    others: List<InstalledCandidate>?,
    onBack: () -> Unit,
    onContinue: (PatchTarget) -> Unit,
    onDownload: (String) -> Unit,
    onInstalled: (InstalledApp) -> Unit,
    pickApk: () -> Unit,
) {
    val layout = ReseamTheme.layout
    ScreenFrame(title = "New patch", onBack = onBack, header = { Stepper(current = 0) }, wide = true, chromeKey = PatchFlowChrome) {
        Column(Modifier.fillMaxSize().padding(horizontal = layout.pageMargin, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StepInstruction("Choose an app to patch")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(layout.gutter), verticalAlignment = Alignment.CenterVertically) {
                SegmentedControl(ModeSegments, state.mode, viewModel::setMode, Modifier.width(240.dp))
                if (state.mode == PickMode.Apps) {
                    TextField(state.query, viewModel::setQuery, placeholder = "Search apps", leading = Icons.Search, modifier = Modifier.width(420.dp))
                }
            }
            if (state.mode == PickMode.File) {
                LazyColumn(Modifier.weight(1f).widthIn(max = layout.contentMaxWidth).fillMaxWidth()) {
                    item { FilePicker(state.selected, state.pickingFile, onPick = { viewModel.pickFile(pickApk) }, onContinue) }
                }
            } else if (catalog == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Spinner() }
            } else {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val library = catalog.installed.isNotEmpty() || catalog.saved.isNotEmpty() || (viewModel.installedSupported && universal > 0)
                    val libraryWidth = (maxWidth * 0.28f).coerceIn(280.dp, 360.dp)
                    val catalogWidth = if (library) maxWidth - libraryWidth - layout.gutter else maxWidth
                    val columns = (catalogWidth / 340.dp).toInt().coerceIn(1, 3)
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(layout.gutter)) {
                        if (library) {
                            LazyColumn(Modifier.width(libraryWidth).fillMaxHeight(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
                                if (catalog.installed.isNotEmpty()) {
                                    item { SectionHeader("Installed", trailing = catalog.installed.size.toString()) }
                                    installedGrid(catalog.installed.matching(state.query), onInstalled, 1, layout.gutter)
                                }
                                if (catalog.saved.isNotEmpty()) {
                                    item { SectionHeader("Saved", trailing = catalog.saved.size.toString()) }
                                    savedGrid(catalog.saved.matching(state.query), onContinue, 1, layout.gutter)
                                }
                                if (viewModel.installedSupported && universal > 0) {
                                    if (!state.showingAll) {
                                        item { Button(onClick = viewModel::showAllApps, variant = ButtonVariant.Subtle, size = ButtonSize.Small) { Text("Show all apps") } }
                                    } else {
                                        item { SectionHeader("Other apps", trailing = others?.size?.toString()) }
                                        if (others == null) item { Spinner() }
                                        else installedGrid(others.matching(state.query), onInstalled, 1, layout.gutter)
                                    }
                                }
                            }
                        }
                        LazyColumn(Modifier.weight(1f).fillMaxHeight(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
                            val downloadable = catalog.downloadable.matching(state.query)
                            item { SectionHeader("Available to download", trailing = downloadable.size.toString()) }
                            if (downloadable.isEmpty()) {
                                item { EmptyState("No apps to download", if (state.query.isNotBlank()) "No apps match your search." else "Pick a local app or browse for an APK file.", icon = Icons.Download) }
                            } else {
                                downloadableGrid(downloadable, onDownload, columns, layout.gutter)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.savedGrid(apps: List<SavedCandidate>, onContinue: (PatchTarget) -> Unit, columns: Int, gutter: Dp) {
    grid(apps, key = { it.apk.id }, columns, gutter) { candidate, modifier ->
        val apk = candidate.apk
        AppRow(apk.name, apk.versionName ?: apk.packageName.orEmpty(), candidate.patchCount, onClick = { onContinue(apk.target()) }, modifier) {
            AppIcon(apk.name, apk.packageName, iconPath = apk.iconPath)
        }
    }
}

private fun LazyListScope.downloadableGrid(apps: List<DownloadCandidate>, onDownload: (String) -> Unit, columns: Int, gutter: Dp) {
    grid(apps, key = { it.packageName }, columns, gutter) { candidate, modifier ->
        AppRow(candidate.packageName, candidate.versions.first().version ?: "Any version", candidate.patchCount, onClick = { onDownload(candidate.packageName) }, modifier) {
            IconTile(Icons.Download, tint = ReseamTheme.colors.mutedForeground)
        }
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

/**
 * An installed app can be patched as it is, mounted over with root, or replaced by another version, so tapping one
 * asks which.
 */
@Composable
private fun InstalledAppSheet(
    app: InstalledApp,
    mountAvailable: Boolean,
    work: SheetWork,
    onDismiss: () -> Unit,
    onUse: () -> Unit,
    onMount: () -> Unit,
    onDownload: () -> Unit,
) {
    Sheet(onDismiss = onDismiss) {
        SheetHeader(app.name, app.versionName?.let { "Installed version $it" } ?: app.packageName)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (work == SheetWork.RootDenied) {
                Banner("Mounting needs root. Allow Reseam Manager in Magisk, KernelSU, or APatch, then try again.")
            }
            val busy = work == SheetWork.Using || work == SheetWork.Mounting
            Button(
                onClick = onUse,
                size = ButtonSize.Large,
                fullWidth = true,
                enabled = !busy,
                icon = if (work == SheetWork.Using) null else Icons.Smartphone,
            ) {
                if (work == SheetWork.Using) Spinner(size = 18)
                Text("Use installed version")
            }
            if (mountAvailable) {
                Button(
                    onClick = onMount,
                    size = ButtonSize.Large,
                    fullWidth = true,
                    variant = ButtonVariant.Subtle,
                    enabled = !busy,
                    icon = if (work == SheetWork.Mounting) null else Icons.Layers,
                ) {
                    if (work == SheetWork.Mounting) Spinner(size = 18)
                    Text("Mount over installed version")
                }
            }
            Button(onClick = onDownload, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Subtle, enabled = !busy, icon = Icons.Download) {
                Text("Download another version")
            }
            if (mountAvailable) {
                Text(
                    text = "Mounting keeps the app's data and Google sign-in. Unmount to get the original back.",
                    style = ReseamTheme.typography.caption,
                    color = ReseamTheme.colors.mutedForeground,
                )
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
