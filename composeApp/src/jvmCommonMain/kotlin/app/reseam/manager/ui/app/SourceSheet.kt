package app.reseam.manager.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import app.reseam.manager.data.AppSource
import app.reseam.manager.data.SavedApk
import app.reseam.manager.data.VersionOption
import app.reseam.manager.resources.*
import app.reseam.manager.ui.byteSize
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.SectionLabel
import app.reseam.manager.ui.components.SelectedMark
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.versionLabel
import java.io.File
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

val AppSource.icon: ImageVector
    get() = when (this) {
        is AppSource.Download -> Icons.Download
        is AppSource.Installed -> Icons.Phone
        is AppSource.Saved -> Icons.Package
        is AppSource.File -> Icons.FolderOpen
    }

@Composable
fun AppSource.title(): String = when (this) {
    is AppSource.Download -> versionName?.let { stringResource(Res.string.source_download, it) } ?: stringResource(Res.string.source_download_newest)
    is AppSource.Installed -> stringResource(Res.string.source_installed)
    is AppSource.Saved -> stringResource(Res.string.source_saved)
    is AppSource.File -> File(path).name
}

@Composable
fun AppSource.version(): String =
    if (this is AppSource.Download && versionName == null) stringResource(Res.string.source_newest) else versionLabel(versionName)

@Composable
fun AppSource.supporting(recommended: VersionOption?): String = when (this) {
    is AppSource.Download -> if (recommended?.versionName == versionName) stringResource(Res.string.source_recommended) else version()
    is AppSource.Installed -> stringResource(Res.string.source_installed_supporting, version())
    is AppSource.Saved, is AppSource.File -> version()
}

@Composable
fun SourceSheet(
    current: AppSource?,
    versions: List<VersionOption>,
    saved: List<SavedApk>,
    installedVersion: String?,
    onChoose: (AppSource) -> Unit,
    onDismiss: () -> Unit,
) {
    Sheet(stringResource(Res.string.app_file), onDismiss) {
        SectionLabel(stringResource(Res.string.source_section_download))
        ItemGroup {
            versions.forEachIndexed { index, option ->
                val source = AppSource.Download(option.versionName)
                item { shape ->
                    val count = if (option.patchCount == 0) stringResource(Res.string.source_any_app_only) else pluralStringResource(Res.plurals.source_supported, option.patchCount, option.patchCount)
                    SourceItem(
                        source = source,
                        title = option.versionName ?: stringResource(Res.string.source_newest),
                        supporting = if (index == 0) stringResource(Res.string.source_recommended_count, count) else count,
                        shape = shape,
                        current = current,
                        onChoose = onChoose,
                    )
                }
            }
        }
        if (saved.isNotEmpty()) {
            SectionLabel(stringResource(Res.string.source_section_saved))
            ItemGroup {
                saved.forEach { apk ->
                    val source = AppSource.Saved(apk.id, apk.versionName)
                    item { shape -> SourceItem(source, versionLabel(apk.versionName), byteSize(apk.sizeBytes), shape, current, onChoose) }
                }
            }
        }
        if (installedVersion != null) {
            val source = AppSource.Installed(installedVersion)
            SectionLabel(stringResource(Res.string.source_section_installed))
            ItemGroup {
                item { shape -> SourceItem(source, source.title(), stringResource(Res.string.source_installed_warning, source.version()), shape, current, onChoose) }
            }
        }
    }
}

@Composable
private fun SourceItem(source: AppSource, title: String, supporting: String, shape: Shape, current: AppSource?, onChoose: (AppSource) -> Unit) {
    val selected = source == current
    GroupedItem(
        title = title,
        supporting = supporting,
        shape = shape,
        icon = source.icon,
        selected = selected,
        onClick = { onChoose(source) },
        trailing = {
            if (selected) SelectedMark()
        },
    )
}
