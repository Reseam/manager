package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.data.Bundle
import app.reseam.manager.data.StagedBundle
import app.reseam.manager.data.TrustPrompt
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonStyle
import app.reseam.manager.ui.components.Chevron
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.Loading
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.fingerprint
import app.reseam.manager.ui.nav.LocalDetailRoute
import app.reseam.manager.ui.nav.Route
import org.jetbrains.compose.resources.stringResource

@Composable
fun SourcesScreen(viewModel: SourcesViewModel, onBack: () -> Unit, onOpen: (Route.Source) -> Unit) {
    val bundles by viewModel.bundles.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val open = LocalDetailRoute.current as? Route.Source
    var adding by rememberSaveable { mutableStateOf(false) }

    SettingsPage(
        title = stringResource(Res.string.sources_title),
        onBack = onBack,
        footer = { Button(stringResource(Res.string.sources_add), { adding = true }, Modifier.weight(1f), style = ButtonStyle.Tonal, icon = Icons.Plus, enabled = !busy) },
    ) {
        val loaded = bundles
        if (loaded == null) {
            item { Loading() }
        } else {
            itemGroup {
                loaded.forEach { bundle ->
                    item(key = bundle.id) { shape ->
                        GroupedItem(
                            title = bundle.name,
                            supporting = bundle.summary(),
                            shape = shape,
                            icon = if (bundle.official) Icons.ShieldCheck else Icons.Layers,
                            selected = open?.id == bundle.id,
                            onClick = { onOpen(Route.Source(bundle.id)) },
                            trailing = { Chevron() },
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        var url by rememberSaveable { mutableStateOf("") }
        Sheet(stringResource(Res.string.sources_add), onDismiss = { adding = false }) {
            Text(stringResource(Res.string.sources_add_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextField(url, { url = it }, stringResource(Res.string.sources_link), Modifier.fillMaxWidth(), keyboardType = KeyboardType.Uri)
            Button(
                label = stringResource(Res.string.sources_add_link),
                onClick = {
                    viewModel.addUrl(url)
                    adding = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank(),
            )
            Button(
                label = stringResource(Res.string.sources_pick),
                onClick = {
                    viewModel.addFile()
                    adding = false
                },
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Tonal,
                icon = Icons.FolderOpen,
            )
        }
    }
    pending?.let { TrustDialog(it, viewModel::decide) }
}

@Composable
private fun Bundle.summary(): String = when {
    official -> stringResource(Res.string.sources_official, version.orEmpty())
    version != null -> stringResource(Res.string.version, version)
    else -> stringResource(Res.string.sources_by, author)
}

@Composable
private fun TrustDialog(staged: StagedBundle, onDecide: (Boolean) -> Unit) {
    val name = staged.metadata.name
    val (headline, body) = when (staged.prompt) {
        TrustPrompt.NewApiSigner -> stringResource(Res.string.trust_server_title) to stringResource(Res.string.trust_server_body, name)
        is TrustPrompt.ChangedSigner -> stringResource(Res.string.trust_changed_title, name) to stringResource(Res.string.trust_changed_body)
        TrustPrompt.UnknownSigner, null -> stringResource(Res.string.trust_unknown_title, name) to stringResource(Res.string.trust_unknown_body)
    }
    Dialog(
        headline = headline,
        body = body,
        icon = Icons.ShieldCheck,
        onDismiss = { onDecide(false) },
        actions = listOf(
            DialogAction(stringResource(Res.string.trust_decline), { onDecide(false) }),
            DialogAction(stringResource(Res.string.trust_accept), { onDecide(true) }),
        ),
    ) {
        Text(stringResource(Res.string.trust_key, staged.metadata.publicKey.fingerprint()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
