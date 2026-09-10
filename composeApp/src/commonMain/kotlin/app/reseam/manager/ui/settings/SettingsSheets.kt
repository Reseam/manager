package app.reseam.manager.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.reseam.manager.data.DefaultApiBaseUrl
import app.reseam.manager.data.SigningKeyInfo
import app.reseam.manager.ui.components.Banner
import app.reseam.manager.ui.components.BannerVariant
import app.reseam.manager.ui.components.Button
import app.reseam.manager.ui.components.ButtonSize
import app.reseam.manager.ui.components.ButtonVariant
import app.reseam.manager.ui.components.Icons
import app.reseam.manager.ui.components.InfoCard
import app.reseam.manager.ui.components.InfoRow
import app.reseam.manager.ui.components.PasswordField
import app.reseam.manager.ui.components.Sheet
import app.reseam.manager.ui.components.SheetHeader
import app.reseam.manager.ui.components.TextField
import app.reseam.manager.ui.theme.ReseamTheme

@Composable
fun ApiBaseUrlSheet(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var draft by rememberSaveable { mutableStateOf(initial) }
    Sheet(onDismiss = onDismiss) {
        SheetHeader("API base URL", "Where the official patch index is fetched from. Change this only for self-hosted or staging servers.")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TextField(value = draft, onValueChange = { draft = it }, placeholder = DefaultApiBaseUrl, leading = Icons.Globe, mono = true)
            Button(onClick = { onSave(draft) }, size = ButtonSize.Large, fullWidth = true, enabled = draft.isNotBlank() && draft.trim() != initial) { Text("Save") }
            Button(onClick = { draft = DefaultApiBaseUrl }, fullWidth = true, variant = ButtonVariant.Ghost, enabled = draft != DefaultApiBaseUrl) { Text("Reset to default") }
        }
    }
}

private enum class SigningAction { Export, Reset }

/** The signing identity: what it is, and moving it in or out as a password-protected keystore. */
@Composable
fun SigningKeySheet(
    info: SigningKeyInfo?,
    onDismiss: () -> Unit,
    onExport: (password: String) -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
) {
    val colors = ReseamTheme.colors
    var action by rememberSaveable { mutableStateOf<SigningAction?>(null) }
    when (action) {
        SigningAction.Export -> PasswordSheet(
            title = "Export keystore",
            body = "Choose a password for the keystore file. You need it to import the key again.",
            confirmLabel = "Save keystore",
            onDismiss = { action = null },
            onConfirm = { onExport(it); onDismiss() },
        )
        SigningAction.Reset -> Sheet(onDismiss = { action = null }) {
            SheetHeader("Reset signing key?", "A new key is created on the next patch. Apps patched with the current key will not update from the new builds; they must be uninstalled first.")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onReset(); onDismiss() }, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Danger, icon = Icons.Trash) { Text("Reset key") }
                Button(onClick = { action = null }, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Ghost) { Text("Keep it") }
            }
        }
        null -> Sheet(onDismiss = onDismiss) {
            SheetHeader(
                title = "Signing key",
                body = "Every patched app is signed with this key. Android only updates an app when the new build carries the same signature, so keep a backup to re-patch after reinstalling Reseam or on another device.",
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (info == null) {
                    Banner("No key yet. It is created the first time you patch an app, or import one now.", variant = BannerVariant.Neutral)
                } else {
                    InfoCard(rows = listOf { InfoRow("SHA-256", info.fingerprint, mono = true) })
                }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { action = SigningAction.Export }, size = ButtonSize.Large, fullWidth = true, enabled = info != null, icon = Icons.Download) { Text("Export keystore") }
                    Button(onClick = { onImport(); onDismiss() }, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Subtle, icon = Icons.Upload) { Text("Import keystore") }
                    Button(onClick = { action = SigningAction.Reset }, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Danger, enabled = info != null) { Text("Reset key") }
                }
                Text("Keystores are PKCS#12 files. Only ECDSA P-256 keys can sign.", style = ReseamTheme.typography.captionSmall, color = colors.mutedForeground)
            }
        }
    }
}

@Composable
fun PasswordSheet(title: String, body: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    Sheet(onDismiss = onDismiss) {
        SheetHeader(title, body)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PasswordField(value = password, onValueChange = { password = it })
            Button(onClick = { onConfirm(password) }, size = ButtonSize.Large, fullWidth = true, enabled = password.isNotEmpty()) { Text(confirmLabel) }
            Button(onClick = onDismiss, size = ButtonSize.Large, fullWidth = true, variant = ButtonVariant.Ghost) { Text("Cancel") }
        }
    }
}
