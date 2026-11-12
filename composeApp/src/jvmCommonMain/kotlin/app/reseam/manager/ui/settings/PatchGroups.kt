package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import app.reseam.manager.AppGraph
import app.reseam.manager.resources.*
import app.reseam.manager.sdk.LocalizedPatch
import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.text
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.components.AppIcon
import app.reseam.manager.ui.components.AppLook
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.Chip
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.IconBadge
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.lookOf
import app.reseam.manager.ui.theme.Sizes
import app.reseam.manager.ui.theme.Space
import app.reseam.sdk.PatchMetadata
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

data class PatchGroup(val app: AppLook?, val patches: List<LocalizedPatch>)

/** The shown patches by app, most patches first, then the ones for any app. */
suspend fun AppGraph.patchGroups(patches: List<PatchMetadata>): List<PatchGroup> {
    val shown = patches.filterNot { it.spec.hidden }.map { LocalizedPatch(it, it.text()) }
    val byApp = shown.flatMap { patch -> patch.meta.declared.map { it.`package` to patch } }
        .groupBy({ it.first }, { it.second })
        .map { (packageName, list) -> PatchGroup(lookOf(packageName), list.distinct()) }
        .sortedByDescending { it.patches.size }
    return byApp + listOfNotNull(shown.filter { it.meta.universal }.takeIf { it.isNotEmpty() }?.let { PatchGroup(null, it) })
}

@Composable
fun PatchGroup.name(): String = app?.name ?: stringResource(Res.string.patches_for_any)

@Composable
fun PatchGroupHeader(group: PatchGroup) {
    Row(Modifier.padding(top = Space.lg, bottom = Space.xs), horizontalArrangement = Arrangement.spacedBy(Space.md), verticalAlignment = Alignment.CenterVertically) {
        PatchGroupIcon(group, Sizes.iconMd)
        Text(group.name(), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(pluralStringResource(Res.plurals.home_patches, group.patches.size, group.patches.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PatchGroupIcon(group: PatchGroup, size: Dp) {
    val app = group.app
    if (app != null) AppIcon(app, size) else IconBadge(Icons.Layers, size)
}

/** A source's patches: app filter chips, then each app's patches. With [onOpen] a row opens its patch. */
fun LazyListScope.patchSection(
    groups: List<PatchGroup>?,
    filter: AppFilter,
    onFilter: (AppFilter) -> Unit,
    query: String = "",
    opened: String? = null,
    onOpen: ((LocalizedPatch) -> Unit)? = null,
) {
    pageSection(Res.string.app_patches)
    if (groups == null) {
        item { Loading() }
        return
    }
    item { AppFilters(groups, filter, onFilter) }
    val visible = groups.filter { filter.shows(it) && it.patches.isNotEmpty() }
    if (visible.isEmpty()) {
        item { StateMessage(Icons.Search, stringResource(Res.string.patches_no_match, query.trim()), null) }
    }
    visible.forEach { group ->
        val groupKey = group.app?.packageName ?: ""
        item(key = "group:$groupKey") { PatchGroupHeader(group) }
        itemGroup {
            group.patches.forEach { patch ->
                item(key = "$groupKey/${patch.meta.spec.id}") { shape ->
                    GroupedItem(
                        title = patch.text.name,
                        supporting = patch.text.description.ifEmpty { null },
                        shape = shape,
                        icon = Icons.Patch,
                        selected = opened == patch.meta.spec.id,
                        onClick = onOpen?.let { { it(patch) } },
                        trailing = onOpen?.let { { Chevron() } },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppFilters(groups: List<PatchGroup>, filter: AppFilter, onFilter: (AppFilter) -> Unit) {
    val options = listOf(AppFilter.All to stringResource(Res.string.source_filter, stringResource(Res.string.source_filter_all), groups.sumOf { it.patches.size })) +
        groups.map { group -> AppFilter.Only(group.app?.packageName) to stringResource(Res.string.source_filter, group.name(), group.patches.size) }
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        items(options, key = { (option) -> option.toString() }) { (option, label) ->
            val selected = option == filter
            Chip(label, Modifier.semantics { this.selected = selected }, outlined = !selected, onClick = { onFilter(option) })
        }
    }
}

@Composable
fun AppsSheet(groups: List<PatchGroup>, onDismiss: () -> Unit, onChoose: (AppFilter) -> Unit) {
    Sheet(stringResource(Res.string.source_apps), onDismiss, supporting = stringResource(Res.string.source_apps_body)) {
        ItemGroup {
            groups.forEach { group ->
                item { shape ->
                    GroupedItem(
                        title = group.name(),
                        supporting = pluralStringResource(Res.plurals.home_patches, group.patches.size, group.patches.size),
                        shape = shape,
                        leading = { PatchGroupIcon(group, Sizes.appIconSm) },
                        onClick = { onChoose(AppFilter.Only(group.app?.packageName)) },
                        trailing = { Chevron() },
                    )
                }
            }
        }
    }
}
