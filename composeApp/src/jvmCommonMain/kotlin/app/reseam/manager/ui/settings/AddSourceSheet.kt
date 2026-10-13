package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.ImportFailure
import app.reseam.manager.data.SourceImport
import app.reseam.manager.data.SourceInput
import app.reseam.manager.data.TrustPrompt
import app.reseam.manager.data.UpdateSource
import app.reseam.manager.resources.*
import app.reseam.manager.sdk.declared
import app.reseam.manager.sdk.universal
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.ItemGroup
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.fingerprint
import app.reseam.manager.ui.nav.Route
import app.reseam.sdk.PatchMetadata
import app.reseam.sdk.Problem
import app.reseam.sdk.SdkError
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val NamedApps = 3

/** The one place a patch source is added, from the Add button, a shared link or an opened file. */
@Composable
fun AddSourceSheet(viewModel: AddSourceViewModel, onOpen: (Route.Source) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.added.collect { onOpen(Route.Source(it)) } }
    val current = state ?: return
    val title = when (current) {
        is SourceImport.Entering -> stringResource(Res.string.sources_add)
        is SourceImport.Reviewing -> stringResource(if (current.replacing != null) Res.string.sources_replace_title else Res.string.trust_unknown_title, current.staged.metadata.name)
        is SourceImport.Present -> stringResource(Res.string.sources_present_title, current.bundle.name)
    }
    val byline = (current as? SourceImport.Reviewing)?.staged?.metadata?.author?.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.sources_by, it) }
    key(current.session) {
        Sheet(title, onDismiss = { viewModel.dismiss(current.session) }, supporting = byline) {
            when (current) {
                is SourceImport.Entering -> EnterSource(current, viewModel)
                is SourceImport.Reviewing -> ReviewSource(current, viewModel)
                is SourceImport.Present -> {
                    Text(stringResource(Res.string.sources_present_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        label = stringResource(Res.string.sources_open),
                        onClick = {
                            viewModel.dismiss(current.session)
                            onOpen(Route.Source(current.bundle.id))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnterSource(state: SourceImport.Entering, viewModel: AddSourceViewModel) {
    val failure = state.failure
    val linkError = failure?.takeIf { it.input is SourceInput.Link }?.describe()
    Text(stringResource(Res.string.sources_add_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextField(
        value = state.link,
        onValueChange = viewModel::edit,
        label = stringResource(Res.string.sources_link),
        modifier = Modifier.fillMaxWidth(),
        error = linkError,
        keyboardType = KeyboardType.Uri,
        onSubmit = { viewModel.add(state.link) }.takeIf { state.link.isNotBlank() && state.checking == null },
    )
    if (failure != null && failure.input is SourceInput.File) {
        Text(failure.describe(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
    }
    Button(
        label = stringResource(Res.string.sources_add_link),
        onClick = { viewModel.add(state.link) },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.link.isNotBlank() && state.checking == null,
        loading = state.checking is SourceInput.Link,
    )
    Button(
        label = stringResource(Res.string.sources_pick),
        onClick = viewModel::pick,
        modifier = Modifier.fillMaxWidth(),
        style = ButtonStyle.Tonal,
        icon = Icons.FolderOpen,
        enabled = state.checking == null,
        loading = state.checking is SourceInput.File,
    )
}

@Composable
private fun ReviewSource(state: SourceImport.Reviewing, viewModel: AddSourceViewModel) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val metadata = state.staged.metadata
    val patches = state.staged.patches.filterNot { it.spec.hidden }
    val newSigner = state.staged.prompt == TrustPrompt.UnknownSigner
    if (metadata.description.isNotBlank()) {
        Text(metadata.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
    ItemGroup {
        item { shape ->
            GroupedItem(stringResource(Res.string.source_apps), shape, supporting = appsSummary(patches, apps), icon = Icons.Phone)
        }
        item { shape ->
            GroupedItem(stringResource(Res.string.app_patches), shape, supporting = pluralStringResource(Res.plurals.home_patches, patches.size, patches.size), icon = Icons.Patch)
        }
        item { shape ->
            val host = (state.staged.source as? UpdateSource.Index)?.url?.toHttpUrlOrNull()?.host
            GroupedItem(
                title = stringResource(Res.string.source_updates),
                shape = shape,
                supporting = host?.let { stringResource(Res.string.sources_updates_from, it) } ?: stringResource(Res.string.source_updates_off),
                icon = Icons.Refresh,
            )
        }
        if (newSigner) {
            item { shape -> GroupedItem(stringResource(Res.string.source_signer), shape, supporting = metadata.publicKey.fingerprint(), icon = Icons.ShieldCheck) }
        }
    }
    val note = when {
        newSigner -> Res.string.trust_unknown_body
        state.replacing != null -> Res.string.sources_replace_body
        else -> null
    }
    note?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Button(
        label = stringResource(if (state.replacing != null) Res.string.sources_replace else Res.string.sources_add_link),
        onClick = viewModel::confirm,
        modifier = Modifier.fillMaxWidth(),
        loading = state.saving,
    )
}

/** A source that can't load is new to the user, so the message says who can fix it rather than asking them to update it. */
@Composable
private fun ImportFailure.describe(): String = when ((error as? SdkError)?.problem) {
    is Problem.BundleTooOld -> stringResource(Res.string.sources_error_outdated)
    is Problem.EngineTooOld -> stringResource(Res.string.sources_error_newer)
    is Problem.UnreadableBundle -> stringResource(Res.string.sources_error_unreadable)
    else -> error.describe()
}

/** Names the apps this device knows and counts the rest, counting all of them until the names are looked up. */
@Composable
private fun appsSummary(patches: List<PatchMetadata>, names: List<String>?): String {
    val count = patches.flatMap { it.declared }.map { it.`package` }.distinct().size
    if (count == 0) return stringResource(Res.string.source_any_app_only)
    val shown = names.orEmpty().take(NamedApps)
    val listed = when {
        shown.isEmpty() -> pluralStringResource(Res.plurals.source_apps_count, count, count)
        count > shown.size -> stringResource(Res.string.sources_apps_more, shown.joinToString(", "), count - shown.size)
        else -> shown.joinToString(", ")
    }
    return if (patches.any { it.universal }) stringResource(Res.string.source_apps_and_any, listed) else listed
}
