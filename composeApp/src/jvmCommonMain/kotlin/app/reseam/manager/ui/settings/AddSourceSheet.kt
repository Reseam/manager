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
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.describe
import app.reseam.manager.ui.nav.Route
import app.reseam.sdk.Problem
import app.reseam.sdk.SdkError
import org.jetbrains.compose.resources.stringResource

/**
 * Where a patch source is entered, from the Add button, a shared link or an opened file. Once it's read,
 * [onReview] shows it on its own page; while that page is open ([reviewing]) the sheet stays closed.
 */
@Composable
fun AddSourceSheet(viewModel: AddSourceViewModel, reviewing: Boolean, onReview: () -> Unit, onOpen: (Route.Source) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state ?: return
    if (current is SourceImport.Reviewing) {
        LaunchedEffect(current.session) { onReview() }
        return
    }
    if (reviewing) return
    key(current.session) {
        when (current) {
            is SourceImport.Entering -> Sheet(stringResource(Res.string.sources_add), onDismiss = { viewModel.dismiss(current.session) }) {
                EnterSource(current, viewModel)
            }
            is SourceImport.Present -> Sheet(stringResource(Res.string.sources_present_title, current.bundle.name), onDismiss = { viewModel.dismiss(current.session) }) {
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

/** A source that can't load is new to the user, so the message says who can fix it rather than asking them to update it. */
@Composable
fun ImportFailure.describe(): String = when ((error as? SdkError)?.problem) {
    is Problem.BundleTooOld -> stringResource(Res.string.sources_error_outdated)
    is Problem.EngineTooOld -> stringResource(Res.string.sources_error_newer)
    is Problem.UnreadableBundle -> stringResource(Res.string.sources_error_unreadable)
    else -> error.describe()
}
