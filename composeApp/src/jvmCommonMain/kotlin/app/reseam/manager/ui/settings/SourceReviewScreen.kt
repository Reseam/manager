package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.CommunityBundle
import app.reseam.manager.data.SourceImport
import app.reseam.manager.data.TrustPrompt
import app.reseam.manager.data.UpdateSource
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.ExternalMark
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.MarkdownText
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.fingerprint
import app.reseam.manager.ui.nav.Route
import app.reseam.manager.ui.theme.Space
import app.reseam.manager.ui.theme.extendedTypography
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Where the patches section starts, after the intro and the details. */
private const val PatchesIndex = 2

/** Shows a patch source before it's added, laid out like an added source's page, with its README when the community list has one. */
@Composable
fun SourceReviewScreen(viewModel: SourceReviewViewModel, onBack: () -> Unit, onOpen: (Route.Source) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listing by viewModel.listing.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val readme by viewModel.readme.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var listingApps by rememberSaveable { mutableStateOf(false) }
    var reading by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // The import ends as the page leaves, so the page keeps showing it, and its listing, while it animates out.
    var shown by remember { mutableStateOf(state to listing) }
    if (state != null) shown = state to listing
    val (current, listed) = shown
    if (current == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val reviewing = current as? SourceImport.Reviewing

    SettingsPage(
        title = reviewing?.staged?.metadata?.name ?: listed?.name ?: stringResource(Res.string.sources_add),
        onBack = onBack,
        state = listState,
        footer = reviewing?.let { review ->
            {
                Button(
                    label = stringResource(if (review.replacing != null) Res.string.sources_replace else Res.string.sources_add_link),
                    onClick = viewModel::confirm,
                    modifier = Modifier.weight(1f),
                    loading = review.saving,
                )
            }
        },
    ) {
        when (current) {
            is SourceImport.Entering -> {
                val failure = current.failure
                if (failure == null) {
                    item { Loading() }
                } else {
                    item {
                        StateMessage(Icons.CircleAlert, stringResource(Res.string.sources_review_failed), failure.describe(), action = DialogAction(stringResource(Res.string.try_again), viewModel::retry))
                    }
                }
            }
            is SourceImport.Present -> item {
                StateMessage(
                    Icons.CircleCheck,
                    stringResource(Res.string.sources_present_title, current.bundle.name),
                    stringResource(Res.string.sources_present_body),
                    action = DialogAction(stringResource(Res.string.sources_open), { onOpen(Route.Source(current.bundle.id)) }),
                )
            }
            is SourceImport.Reviewing -> {
                item { Intro(current, listed) }
                item {
                    Details(
                        review = current,
                        listing = listed,
                        groups = groups,
                        hasReadme = readme != null,
                        onApps = { listingApps = true },
                        onReadme = { reading = true },
                    )
                }
                patchSection(groups, filter, viewModel::setFilter)
            }
        }
    }

    if (listingApps) {
        AppsSheet(groups.orEmpty(), onDismiss = { listingApps = false }) { chosen ->
            listingApps = false
            viewModel.setFilter(chosen)
            scope.launch { listState.animateScrollToItem(PatchesIndex) }
        }
    }
    readme?.takeIf { reading }?.let { text ->
        Sheet(stringResource(Res.string.sources_readme), onDismiss = { reading = false }) {
            SelectionContainer { MarkdownText(text) }
        }
    }
}

@Composable
private fun Intro(review: SourceImport.Reviewing, listing: CommunityBundle?) {
    val metadata = review.staged.metadata
    val note = when {
        listing != null -> stringResource(Res.string.sources_listed, metadata.author)
        review.replacing != null -> stringResource(Res.string.sources_replace_body)
        review.staged.prompt == TrustPrompt.UnknownSigner -> stringResource(Res.string.trust_unknown_body)
        else -> null
    }
    Column(Modifier.padding(horizontal = Space.lg, vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        if (metadata.author.isNotBlank()) {
            Text(stringResource(Res.string.sources_by, metadata.author), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (metadata.description.isNotBlank()) {
            Text(metadata.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Details(
    review: SourceImport.Reviewing,
    listing: CommunityBundle?,
    groups: List<PatchGroup>?,
    hasReadme: Boolean,
    onApps: () -> Unit,
    onReadme: () -> Unit,
) {
    val metadata = review.staged.metadata
    val host = (review.staged.source as? UpdateSource.Index)?.url?.toHttpUrlOrNull()?.host
    val uriHandler = LocalUriHandler.current
    val source = (listing?.repository ?: review.staged.origin).takeIf { it.startsWith("https://") || it.startsWith("http://") }
    val homepage = listing?.homepage?.takeIf { it.trimEnd('/') != listing.repository }
    ItemGroup {
        item { shape ->
            GroupedItem(
                title = stringResource(Res.string.source_version),
                shape = shape,
                supporting = metadata.version.ifEmpty { stringResource(Res.string.source_version_none) },
                icon = Icons.Package,
            )
        }
        item { shape ->
            GroupedItem(
                title = stringResource(Res.string.source_updates),
                shape = shape,
                supporting = host?.let { stringResource(Res.string.sources_updates_from, it) } ?: stringResource(Res.string.source_updates_off),
                icon = Icons.Refresh,
            )
        }
        if (source != null) {
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.source_from),
                    shape = shape,
                    supporting = source.removePrefix("https://").removePrefix("http://"),
                    icon = Icons.Source,
                    onClick = { uriHandler.openUri(source) },
                    trailing = { ExternalMark() },
                )
            }
        }
        if (homepage != null) {
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.source_homepage),
                    shape = shape,
                    supporting = homepage.removePrefix("https://").removePrefix("http://"),
                    icon = Icons.Globe,
                    onClick = { uriHandler.openUri(homepage) },
                    trailing = { ExternalMark() },
                )
            }
        }
        item { shape ->
            GroupedItem(
                title = stringResource(Res.string.source_signer),
                shape = shape,
                supporting = metadata.publicKey.fingerprint(),
                supportingStyle = MaterialTheme.extendedTypography.monoSmall,
                icon = Icons.ShieldCheck,
            )
        }
        if (groups != null) {
            item { shape ->
                val apps = groups.count { it.app != null }
                val count = pluralStringResource(Res.plurals.source_apps_count, apps, apps)
                GroupedItem(
                    title = stringResource(Res.string.source_apps),
                    supporting = if (groups.any { it.app == null }) stringResource(Res.string.source_apps_and_any, count) else count,
                    shape = shape,
                    icon = Icons.Phone,
                    onClick = onApps,
                    trailing = { Chevron() },
                )
            }
        }
        if (hasReadme) {
            item { shape ->
                GroupedItem(
                    title = stringResource(Res.string.sources_readme),
                    supporting = stringResource(Res.string.sources_readme_supporting, metadata.author),
                    shape = shape,
                    icon = Icons.BookOpen,
                    onClick = onReadme,
                    trailing = { Chevron() },
                )
            }
        }
    }
}
