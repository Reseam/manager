package app.reseam.manager.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.reseam.manager.platform.HumanCheck
import app.reseam.manager.resources.*
import app.reseam.manager.ui.components.Sheet
import org.jetbrains.compose.resources.stringResource

/** The download site's check that a person is there; what waited on it continues once [onVerified]. */
@Composable
fun HumanCheckSheet(url: String, onVerified: () -> Unit, onDismiss: () -> Unit) {
    Sheet(stringResource(Res.string.check_title), onDismiss = onDismiss) {
        Text(stringResource(Res.string.check_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HumanCheck(url, onVerified = onVerified)
    }
}
