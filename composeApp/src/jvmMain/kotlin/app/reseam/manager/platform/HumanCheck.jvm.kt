package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import app.reseam.manager.ui.components.SheetHeader

@Composable
actual fun HumanCheck(url: String, onVerified: () -> Unit) {
    SheetHeader("A check is needed first", "The desktop app can't show that check yet. Try again in a while, or download the APK yourself and choose it as a file.")
}
