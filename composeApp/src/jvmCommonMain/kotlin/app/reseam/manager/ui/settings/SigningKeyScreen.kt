package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.reseam.manager.platform.rememberClipboardCopy
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.ConfirmDialog
import app.reseam.manager.ui.components.Dialog
import app.reseam.manager.ui.components.DialogAction
import app.reseam.manager.ui.components.GroupedItem
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.StateMessage
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.components.itemGroup
import app.reseam.manager.ui.theme.Space
import org.jetbrains.compose.resources.stringResource

@Composable
fun SigningKeyScreen(viewModel: SigningKeyViewModel, onBack: () -> Unit) {
    val key by viewModel.key.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val copy = rememberClipboardCopy()
    var exporting by rememberSaveable { mutableStateOf(false) }
    var resetting by rememberSaveable { mutableStateOf(false) }
    val fingerprintLabel = stringResource(Res.string.signing_fingerprint)

    SettingsPage(stringResource(Res.string.settings_signing), onBack) {
        val current = key
        if (current == null) {
            item { StateMessage(Icons.Key, stringResource(Res.string.signing_none_title), stringResource(Res.string.signing_none_body)) }
        } else {
            itemGroup {
                item { shape ->
                    GroupedItem(
                        title = fingerprintLabel,
                        supporting = current.fingerprint,
                        shape = shape,
                        icon = Icons.Key,
                        onClick = { copy(fingerprintLabel, current.fingerprint) },
                        trailing = { Icon(Icons.Copy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    )
                }
            }
        }
        itemGroup(top = Space.md) {
            if (current != null) {
                item { shape -> GroupedItem(stringResource(Res.string.signing_export), shape, supporting = stringResource(Res.string.signing_export_supporting), icon = Icons.Upload, onClick = { exporting = true }) }
            }
            item { shape -> GroupedItem(stringResource(Res.string.signing_import), shape, supporting = stringResource(Res.string.signing_import_supporting), icon = Icons.Download, onClick = viewModel::pickKeystore) }
            if (current != null) {
                item { shape ->
                    GroupedItem(
                        title = stringResource(Res.string.signing_reset),
                        shape = shape,
                        supporting = stringResource(Res.string.signing_reset_supporting),
                        icon = Icons.Trash,
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { resetting = true },
                    )
                }
            }
        }
    }

    if (exporting) {
        PasswordDialog(stringResource(Res.string.signing_export_title), stringResource(Res.string.signing_export_body), stringResource(Res.string.save), onDismiss = { exporting = false }) {
            exporting = false
            viewModel.export(it)
        }
    }
    if (importing != null) {
        PasswordDialog(stringResource(Res.string.signing_import_title), stringResource(Res.string.signing_import_body), stringResource(Res.string.signing_import), onDismiss = viewModel::cancelImport, onConfirm = viewModel::import)
    }
    if (resetting) {
        ConfirmDialog(
            headline = stringResource(Res.string.signing_reset_title),
            body = stringResource(Res.string.signing_reset_body),
            icon = Icons.Warning,
            confirm = stringResource(Res.string.signing_reset),
            onConfirm = viewModel::reset,
            onDismiss = { resetting = false },
        )
    }
}

@Composable
private fun PasswordDialog(headline: String, body: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    Dialog(
        headline = headline,
        body = body,
        icon = Icons.Key,
        onDismiss = onDismiss,
        actions = listOf(
            DialogAction(stringResource(Res.string.cancel), onDismiss),
            DialogAction(confirm, { onConfirm(password) }, enabled = password.isNotEmpty()),
        ),
    ) {
        TextField(password, { password = it }, stringResource(Res.string.signing_password), Modifier.fillMaxWidth(), secret = true)
    }
}
