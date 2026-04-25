package app.reseam.manager.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

// Returns a Painter for the system-loaded launcher icon of the given package
// when one is available (Android), null otherwise. Callers fall back to the
// letter-tile AppIconView.
@Composable
expect fun rememberPackageAppIconPainter(packageName: String?): Painter?
