package app.reseam.manager.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.PatchedApp
import app.reseam.manager.resources.*
import app.reseam.manager.ui.ArtifactTask
import app.reseam.manager.ui.ReplaceDialog
import app.reseam.manager.ui.components.AppHeader
import app.reseam.manager.ui.components.BottomBarLayout
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.ConfirmDialog
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.PatchesCard
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.Spinner
import app.reseam.manager.ui.components.SwitchItem
import app.reseam.manager.ui.components.TopBar
import app.reseam.manager.ui.deliverIcon
import app.reseam.manager.ui.deliverLabel
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.nav.SharedKeys
import app.reseam.manager.ui.nav.shared
import app.reseam.manager.ui.shareLabel
import app.reseam.manager.ui.theme.Layout
import app.reseam.manager.ui.theme.Radius
import app.reseam.manager.ui.theme.Space
import app.reseam.sdk.InstallMethod
import app.reseam.sdk.PatchPreset
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val PreviewChips = 4

@Composable
fun AppPageScreen(viewModel: AppPageViewModel, onBack: () -> Unit, onSelectPatches: () -> Unit, onRun: (Route.Run) -> Unit) {
    val session = viewModel.session
    val app by session.app.collectAsStateWithLifecycle()
    val source by session.source.collectAsStateWithLifecycle()
    val editor by session.editor.collectAsStateWithLifecycle()
    val versions by session.versions.collectAsStateWithLifecycle()
    val mount by session.mount.collectAsStateWithLifecycle()
    val patched by viewModel.patched.collectAsStateWithLifecycle()
    val canMount by viewModel.canMount.collectAsStateWithLifecycle()
    val busy by viewModel.tasks.busy.collectAsStateWithLifecycle()
    val updatingPatches by viewModel.updatingPatches.collectAsStateWithLifecycle()
    var choosingSource by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val askRunPermissions = rememberRunPermissionRequest(viewModel::shouldAskRunPermissions, viewModel::runPermissionsAsked)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer).windowInsetsPadding(WindowInsets.statusBars)) {
        Box {
            AppHeader(app, source?.version().orEmpty(), Modifier.shared(SharedKeys.app(app.packageName)))
            TopBar(title = null, onBack = onBack)
        }
        BottomBarLayout(
            bar = {
                Button(
                    label = stringResource(if (patched != null) Res.string.app_patch_again else Res.string.app_patch),
                    onClick = {
                        askRunPermissions()
                        viewModel.run()?.let(onRun)
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Hammer,
                    enabled = busy == null && !updatingPatches && editor?.enabled?.isNotEmpty() == true && source != null,
                )
            },
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(topStart = Radius.md, topEnd = Radius.md)).background(MaterialTheme.colorScheme.surface),
        ) { padding ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(start = Layout.margin, end = Layout.margin, top = Space.xxl, bottom = Space.lg),
                verticalArrangement = Arrangement.spacedBy(Space.md),
            ) {
                patched?.let { PatchedSection(it, viewModel, onRemove = { removing = true }) }
                SectionLabel(stringResource(Res.string.app_patches))
                editor?.let { editor ->
                    val forApp = editor.forApp
                    val enabled = forApp.filter { it.enabled }
                    PatchesCard(
                        title = pluralStringResource(Res.plurals.app_selected, forApp.size, enabled.size, forApp.size),
                        supporting = stringResource(
                            when (editor.preset) {
                                PatchPreset.RECOMMENDED -> Res.string.preset_recommended
                                PatchPreset.ALL -> Res.string.preset_all
                                PatchPreset.NONE -> Res.string.preset_none
                                null -> Res.string.preset_custom
                            },
                        ),
                        chips = enabled.take(PreviewChips).map { it.meta.spec.name },
                        more = (enabled.size - PreviewChips).takeIf { it > 0 }?.let { stringResource(Res.string.app_more, it) },
                        onClick = onSelectPatches,
                    )
                }
                SectionLabel(stringResource(Res.string.app_file), Modifier.padding(top = Space.sm))
                ItemGroup {
                    source?.let { current ->
                        item { shape ->
                            GroupedItem(
                                title = current.title(),
                                supporting = current.supporting(versions.firstOrNull()),
                                shape = shape,
                                icon = current.icon,
                                onClick = { choosingSource = true },
                                trailing = { Chevron() },
                            )
                        }
                    }
                    if (canMount) {
                        item { shape ->
                            SwitchItem(stringResource(Res.string.app_mount), mount, session::setMount, shape, supporting = stringResource(Res.string.app_mount_supporting), icon = Icons.Layers)
                        }
                    }
                }
            }
        }
    }

    if (choosingSource) {
        val saved by viewModel.savedApks.collectAsStateWithLifecycle()
        val installedVersion by viewModel.installedVersion.collectAsStateWithLifecycle()
        SourceSheet(
            current = source,
            versions = versions,
            saved = saved,
            installedVersion = installedVersion,
            onChoose = {
                session.choose(it)
                choosingSource = false
            },
            onDismiss = { choosingSource = false },
        )
    }
    if (viewModel.tasks.conflict.collectAsStateWithLifecycle().value != null) ReplaceDialog(app.name, viewModel.tasks::replace, viewModel.tasks::keepInstalled)
    if (removing) {
        val mounted = viewModel.mount.collectAsStateWithLifecycle().value == MountStatus.Mounted
        ConfirmDialog(
            headline = stringResource(Res.string.remove_title, app.name),
            body = stringResource(if (mounted) Res.string.remove_body_mounted else Res.string.remove_body),
            icon = Icons.Trash,
            confirm = stringResource(Res.string.remove),
            onConfirm = { viewModel.tasks.remove(onBack) },
            onDismiss = { removing = false },
        )
    }
}

@Composable
private fun ColumnScope.PatchedSection(app: PatchedApp, viewModel: AppPageViewModel, onRemove: () -> Unit) {
    val installed by viewModel.patchedInstalled.collectAsStateWithLifecycle()
    val mount by viewModel.mount.collectAsStateWithLifecycle()
    val busy by viewModel.tasks.busy.collectAsStateWithLifecycle()
    val outputExists by viewModel.outputExists.collectAsStateWithLifecycle()
    SectionLabel(stringResource(Res.string.app_patched))
    ItemGroup(Modifier.padding(bottom = Space.sm)) {
        if (installed && viewModel.canOpenApps) {
            item { shape ->
                GroupedItem(stringResource(Res.string.patched_open, app.name), shape, icon = Icons.ExternalLink, onClick = viewModel::openPatched)
            }
        }
        if (app.installMethod == InstallMethod.MOUNT) {
            item { shape ->
                SwitchItem(
                    title = stringResource(Res.string.app_mount),
                    checked = mount == MountStatus.Mounted,
                    onCheckedChange = { if (it) viewModel.tasks.mount() else viewModel.tasks.unmount() },
                    shape = shape,
                    supporting = when (val status = mount) {
                        MountStatus.Mounted -> stringResource(Res.string.mount_on)
                        MountStatus.NotInstalled -> stringResource(Res.string.mount_not_installed, app.name)
                        is MountStatus.Updated -> stringResource(Res.string.mount_updated, status.installedVersion.orEmpty())
                        MountStatus.NotMounted, MountStatus.Checking -> stringResource(Res.string.mount_off)
                    },
                    icon = Icons.Layers,
                    enabled = busy == null && (mount == MountStatus.Mounted || mount == MountStatus.NotMounted && outputExists),
                )
            }
        } else if (outputExists) {
            item { shape ->
                GroupedItem(
                    title = viewModel.artifactKind.deliverLabel(),
                    supporting = pluralStringResource(Res.plurals.patched_summary, app.patches.size, app.patches.size, app.versionName.orEmpty()),
                    shape = shape,
                    icon = viewModel.artifactKind.deliverIcon,
                    onClick = viewModel.tasks::deliver.takeIf { busy == null },
                    trailing = { if (busy == ArtifactTask.Deliver) Spinner() },
                )
            }
        }
        if (outputExists) {
            item { shape ->
                GroupedItem(
                    title = viewModel.artifactKind.shareLabel(),
                    shape = shape,
                    icon = Icons.Upload,
                    onClick = viewModel.tasks::share.takeIf { busy == null },
                    trailing = { if (busy == ArtifactTask.Share) Spinner() },
                )
            }
        }
        item { shape ->
            GroupedItem(
                title = stringResource(Res.string.patched_remove),
                shape = shape,
                icon = Icons.Trash,
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onRemove.takeIf { busy == null },
                trailing = { if (busy == ArtifactTask.Remove) Spinner(color = MaterialTheme.colorScheme.error) },
            )
        }
    }
}
