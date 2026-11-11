package app.reseam.manager.platform

import androidx.compose.runtime.Composable
import java.util.Locale

/** Makes [language], a BCP 47 tag, the app's locale, or the device's when null, and returns it. */
@Composable
expect fun rememberAppLocale(language: String?): Locale
